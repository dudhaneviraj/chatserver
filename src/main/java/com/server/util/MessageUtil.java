package com.server.util;

import com.server.handler.tcp.TCPHandler;
import com.server.handler.web.WebHandler;
import com.server.state.ChatRoom;
import com.server.state.Manager;
import com.server.state.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.concurrent.ConcurrentHashMap;

public class MessageUtil {


    Manager MANAGER;
    boolean isWeb;

    String newLine;
    public MessageUtil(boolean isWeb,Manager manager)
    {
        this.MANAGER=manager;
        this.isWeb=isWeb;
        newLine=(isWeb)?"<br>":"\n";
    }

    public void write(ChannelHandlerContext ctx,String msg)
    {
        if(isWeb)
            ctx.writeAndFlush(new TextWebSocketFrame(msg));
        else
            ctx.writeAndFlush(msg+newLine);
    }

    public void write(Channel ctx, String msg)
    {
        if(isWeb)
            ctx.writeAndFlush(new TextWebSocketFrame(msg));
        else
            ctx.writeAndFlush(msg);
    }

    public void firstLogin(TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx,"NAME ALREADY IN USE! REENTER LOGIN NAME:");
        else {
            User user = new User(msg);
            tcpHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx,"WELCOME " + user.getUserName()+"!");
        }
    }

    public void firstLogin(WebHandler serverHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx,"NAME ALREADY IN USE! REENTER LOGIN NAME:");
        else {
            User user = new User(msg);
            serverHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx,"WELCOME " + user.getUserName() + "!\n");
        }
    }

    public void joinChatRoom(TCPHandler tcpHandler, ChannelHandlerContext ctx, String msg) {
        User user= tcpHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim());
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel());
        write(ctx,"ENTERING CHAT ROOM: " + chatRoom.getName());
        getUsers(user,ctx);
        write(ctx,"END OF LIST.");
        broadCastMessage(user,ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());

    }

    public void joinChatRoom(WebHandler serverHandler, ChannelHandlerContext ctx, String msg) {
        User user=serverHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim());
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel());
        write(ctx,"ENTERING CHAT ROOM: " + chatRoom.getName() + "\n");
        getUsers(user,ctx);
        write(ctx,"END OF LIST.\n");
        broadCastMessage(user,ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());
    }

    public void leaveChatRoom(User user,ChannelHandlerContext ctx) {
        if (user.getChatRoom() != null) {
            broadCast(user,ctx, "GOTTA GO!");
            broadCastMessage(user,ctx, "* USER HAS LEFT CHAT: " + user.getUserName());
            user.getChatRoom().removeUser(user.getUserName(), ctx.channel());
            user.leaveChatRoom();
        }
    }

    public void broadCastMessage(User user, ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getChannels();
        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c,msg + '\n');
            else
                write(c,msg + " (** THIS IS YOU)\n");
        });
    }

    public void broadCast(User user,ChannelHandlerContext ctx, String msg) {

        ChannelGroup channelGroup = user.getChatRoom().getChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c,"[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n');
            else
                write(c,"[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n');
        });

    }

    public void getUsers(User user,ChannelHandlerContext ctx) {
        user.getChatRoom().getChatRoomUsers().forEach(p ->
        {
            if (user.getUserName().equals(p))
                write(ctx,"* " + p + " (** THIS IS YOU)");
            else
                write(ctx,"* " + p );
        });
    }

    public void getChatRooms(ChannelHandlerContext ctx) {
        ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = MANAGER.getChatRoomsMap();
        if (chatRoomConcurrentHashMap.size() == 0)
        {
            write(ctx,"NO ACTIVE CHAT ROOMS!");
            return;
        }
        write(ctx,"ACTIVE CHATROOMS ARE:");
        chatRoomConcurrentHashMap.entrySet()
                .forEach(
                        p -> write(ctx,p.getKey() + " (" + p.getValue().getChannels().size() + ")" )
                );
        write(ctx,"END OF LIST.");
    }

}
