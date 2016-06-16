package com.server.util;

import com.server.handler.tcp.TCPHandler;
import com.server.handler.web.WebHandler;
import com.server.state.ChatRoom;
import com.server.state.Manager;
import com.server.state.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.concurrent.ConcurrentHashMap;

public class MessageUtil {

    Manager MANAGER;
    boolean isWeb;

    String newLine;

    public MessageUtil(boolean isWeb, Manager manager) {
        this.MANAGER = manager;
        this.isWeb = isWeb;
        newLine = (isWeb) ? "<br>" : "\n";
    }

    public void write(ChannelHandlerContext ctx, String msg, boolean web) {
        if (web)
            ctx.writeAndFlush(new TextWebSocketFrame(msg));
        else
            ctx.writeAndFlush(msg + "\n");
    }

    public void write(Channel ctx, String msg, boolean web) {
        if (web)
            ctx.writeAndFlush(new TextWebSocketFrame(msg));
        else
            ctx.writeAndFlush(msg + "\n");
    }

    public void firstLogin(TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx, "NAME ALREADY IN USE! REENTER LOGIN NAME:", isWeb);
        else {
            User user = new User(msg);
            tcpHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx, "WELCOME " + user.getUserName() + "!", isWeb);
        }
    }

    public void firstLogin(WebHandler serverHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx, "NAME ALREADY IN USE! REENTER LOGIN NAME:", isWeb);
        else {
            User user = new User(msg);
            serverHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx, "WELCOME " + user.getUserName() + "!\n", isWeb);
        }
    }

    public void joinChatRoom(TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {
        User user = tcpHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim(), isWeb);
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel(), isWeb);
        write(ctx, "ENTERING CHAT ROOM: " + chatRoom.getName(), isWeb);
        getUsers(user, ctx);
        write(ctx, "END OF LIST.", isWeb);
        broadCastMessage(user, ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName() + "\n");

    }

    public void joinChatRoom(WebHandler serverHandler, ChannelHandlerContext ctx, String msg) {
        User user = serverHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim(), isWeb);
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel(), isWeb);
        write(ctx, "ENTERING CHAT ROOM: " + chatRoom.getName() + "\n", isWeb);
        broadCastMessage(user, ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());
    }

    public void leaveChatRoom(User user, ChannelHandlerContext ctx) {
        if (user.getChatRoom() != null) {
            broadCast(user, ctx, "GOTTA GO!");
            broadCastMessage(user, ctx, "* USER HAS LEFT CHAT: " + user.getUserName() + "\n");
            user.getChatRoom().removeUser(user.getUserName(), ctx.channel(), isWeb);
            MANAGER.removeChatRoom(user.getChatRoom().getName());
            user.leaveChatRoom();
        }
    }

    public void broadCastMessage(User user, ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();
        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, msg, true);
            else
                write(c, msg + " (** THIS IS YOU)", true);
        });

        channelGroup = user.getChatRoom().getTCPChannels();
        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, msg, false);
            else
                write(c, msg + " (** THIS IS YOU)", false);
        });
    }

    public void broadCast(User user, ChannelHandlerContext ctx, String msg) {

        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n', true);
            else
                write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n', true);
        });


        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n', false);
            else
                write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n', false);
        });
    }

    public void messageUser(User user,String username,ChannelHandlerContext ctx,String msg)
    {
        ChannelId id=user.getChatRoom().getChannelId(username);
        if(id==null) {
            write(ctx,"[SYSTEM] NO SUCH USER",false);
            return;
        }
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();
        channelGroup.stream().forEach(c -> {
                if(c.id().asLongText().equals(id.asLongText()) && c !=ctx.channel())
                        write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] [PERSONAL]" + msg + '\n', true);
        });

        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if(c.id().asLongText().equals(id.asLongText()) && c != ctx.channel())
                    write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] [PERSONAL]" + msg + '\n', false);
        });
        write(ctx, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] [TO USER:"+username+"]" + msg + '\n', isWeb);
    }

    public void getUsers(User user, ChannelHandlerContext ctx) {
        user.getChatRoom().getChatRoomUsers().forEach(p ->
        {
            if (user.getUserName().equals(p))
                write(ctx, "* " + p + " (** THIS IS YOU)", isWeb);
            else
                write(ctx, "* " + p, isWeb);
        });
    }

    public void getChatRooms(ChannelHandlerContext ctx) {
        ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = MANAGER.getChatRoomsMap();
        if (chatRoomConcurrentHashMap.size() == 0) {
            write(ctx, "NO ACTIVE CHAT ROOMS!", isWeb);
            return;
        }
        write(ctx, "ACTIVE CHATROOMS ARE:", isWeb);

        chatRoomConcurrentHashMap.entrySet()
                .forEach(
                        p -> write(ctx, "* " + p.getKey() + " (" + (p.getValue().getWebChannels().size() + p.getValue().getTCPChannels().size()) + ")", isWeb)
                );
        write(ctx, "END OF LIST.", isWeb);
    }

}
