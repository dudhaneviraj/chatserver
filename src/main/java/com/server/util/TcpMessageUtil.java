package com.server.util;

import com.server.handler.tcp.TCPHandler;
import com.server.state.ChatRoom;
import com.server.state.Manager;
import com.server.state.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;

import java.util.concurrent.ConcurrentHashMap;

public class TcpMessageUtil {

    Manager MANAGER = Manager.getInstance();

    static TcpMessageUtil tcpMessageUtil = new TcpMessageUtil();

    private TcpMessageUtil() {
    }

    public static TcpMessageUtil getInstance() {
        return tcpMessageUtil;
    }

    public void write(ChannelHandlerContext ctx, String msg) {
        ctx.writeAndFlush(msg + "\n");
    }

    public void write(Channel ctx, String msg) {
        ctx.writeAndFlush(msg + "\n");
    }

    public void firstLogin(TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx, "NAME ALREADY IN USE! REENTER LOGIN NAME:");
        else {
            User user = new User(msg);
            tcpHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx, "WELCOME " + user.getUserName() + "!");
        }
    }


    public void joinChatRoom(WebMessageUtil webMessageUtil, TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {
        User user = tcpHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim(), false);
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel(), false);
        write(ctx, "ENTERING CHAT ROOM: " + chatRoom.getName());
        getUsers(user, ctx);
        write(ctx, "END OF LIST.");
        broadCastMessage(webMessageUtil, user, ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());

    }


    public void leaveChatRoom(WebMessageUtil webMessageUtil, User user, ChannelHandlerContext ctx) {
        if (user.getChatRoom() != null) {
            broadCast(webMessageUtil, user, ctx, "GOTTA GO!");
            broadCastMessage(webMessageUtil, user, ctx, "* USER HAS LEFT CHAT: " + user.getUserName());
            user.getChatRoom().removeUser(user.getUserName(), ctx.channel(), false);
            MANAGER.removeChatRoom(user.getChatRoom().getName());
            user.leaveChatRoom();
        }
    }

    public void broadCastMessage(WebMessageUtil webMessageUtil, User user, ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                webMessageUtil.write(c, user.getUserName(), user.getChatRoom().getName(), msg);
            else
                webMessageUtil.write(c, "SYSTEM", user.getChatRoom().getName(), msg + " (** THIS IS YOU)");
        });

        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
            else
                write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + " (** THIS IS YOU)");
        });
    }

    public void broadCast(WebMessageUtil webMessageUtil, User user, ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                webMessageUtil.write(c, user.getUserName(), user.getChatRoom().getName(), msg);
            else
                webMessageUtil.write(c, "YOU", user.getChatRoom().getName(), msg);
        });


        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
            else
                write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
        });
    }

    public void messageUser(WebMessageUtil webMessageUtil, User user, String username, ChannelHandlerContext ctx, String msg) {
        ChannelId id = user.getChatRoom().getChannelId(username);
        if (id == null) {
            write(ctx, "[SYSTEM] NO SUCH USER");
            return;
        }
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();
        channelGroup.stream().forEach(c -> {
            if (c.id().asLongText().equals(id.asLongText()) && c != ctx.channel())
                webMessageUtil.write(c, user.getUserName(), user.getChatRoom().getName(), msg);
        });

        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c.id().asLongText().equals(id.asLongText()) && c != ctx.channel())
                write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] [PERSONAL]" + msg);
        });
        write(ctx, "[USER: YOU] [ROOM: " + user.getChatRoom() + "][TO: " + username + "][PERSONAL] " + msg);
    }

    public void getUsers(User user, ChannelHandlerContext ctx) {
        user.getChatRoom().getChatRoomUsers().forEach(p ->
        {
            if (user.getUserName().equals(p))
                write(ctx, "* " + p + " (** THIS IS YOU)");
            else
                write(ctx, "* " + p);
        });
    }

    public void getChatRooms(ChannelHandlerContext ctx) {
        ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = MANAGER.getChatRoomsMap();
        if (chatRoomConcurrentHashMap.size() == 0) {
            write(ctx, "NO ACTIVE CHAT ROOMS!");
            return;
        }
        write(ctx, "ACTIVE CHATROOMS ARE:");

        chatRoomConcurrentHashMap.entrySet()
                .forEach(
                        p -> write(ctx, "* " + p.getKey() + " (" + (p.getValue().getWebChannels().size() + p.getValue().getTCPChannels().size()) + ")")
                );
        write(ctx, "END OF LIST.");
    }

}
