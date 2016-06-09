package com.server.handler;

import com.server.state.ChatRoom;
import com.server.state.Manager;
import com.server.state.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;


public class ServerHandler extends SimpleChannelInboundHandler<String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    static final Manager MANAGER = Manager.getInstance();

    User user = null;

    boolean enableSSL = false;

    public ServerHandler(boolean enableSSL) {
        this.enableSSL = enableSSL;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {

        if (enableSSL) {
            ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                    future -> {
                        ctx.writeAndFlush(
                                "WELCOME TO SSL ENABLED " + InetAddress.getLocalHost().getHostName() + " CHAT SERVER!\n");
                        ctx.writeAndFlush("LOGIN NAME:");

                    });
        } else {
            ctx.writeAndFlush(
                    "WELCOME TO " + InetAddress.getLocalHost().getHostName() + " CHAT SERVER!\n");
            ctx.writeAndFlush("LOGIN NAME:");
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (user == null) {
            firstLogin(ctx, msg);
            return;
        }

        if (msg.toLowerCase().trim().equals("/rooms")) {
            ctx.writeAndFlush(getChatRooms());
            return;
        }

        if (msg.toLowerCase().trim().startsWith("/join") && !msg.toLowerCase().trim().endsWith("/join")) {
            leaveChatRoom(ctx);
            joinChatRoom(ctx, msg);
            return;
        }

        if ("/quit".equals(msg.toLowerCase().trim())) {
            leaveChatRoom(ctx);
            MANAGER.removeUser(user.getUserName());
            ctx.writeAndFlush("BYE!\n");
            ctx.close();
            return;
        }

        if (user.getChatRoom() == null) {
            ctx.writeAndFlush("SELECT CHAT ROOM FIRST!\n");
            return;
        }

        if (msg.toLowerCase().trim().equals("/leave")) {
            leaveChatRoom(ctx);
            return;
        }

        broadCast(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {

        if (user != null) {
            leaveChatRoom(ctx);
            MANAGER.removeUser(user.getUserName());
        }
        ctx.fireChannelInactive();
    }

    public void firstLogin(ChannelHandlerContext ctx, String msg) {
        if (MANAGER.userExists(msg))
            ctx.writeAndFlush("NAME ALREADY IN USE! REENTER LOGIN NAME:");
        else {
            user = new User(msg);
            MANAGER.addUser(user.getUserName());
            ctx.writeAndFlush("WELCOME " + user.getUserName() + "!\n");
        }
    }

    public void joinChatRoom(ChannelHandlerContext ctx, String msg) {
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim());
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel());
        ctx.writeAndFlush("ENTERING CHAT ROOM: " + chatRoom.getName() + "\n");
        getUsers(ctx);
        ctx.writeAndFlush("END OF LIST.\n");
        broadCastMessage(ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());

    }

    public void leaveChatRoom(ChannelHandlerContext ctx) {
        if (user.getChatRoom() != null) {
            broadCast(ctx, "GOTTA GO!");
            broadCastMessage(ctx, "* USER HAS LEFT CHAT: " + user.getUserName());
            user.getChatRoom().removeUser(user.getUserName(), ctx.channel());
            user.leaveChatRoom();
        }
    }

    public void broadCastMessage(ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                c.writeAndFlush(msg + '\n');
            else
                c.writeAndFlush(msg + " (** THIS IS YOU)\n");
        });
    }

    public void broadCast(ChannelHandlerContext ctx, String msg) {

        ChannelGroup channelGroup = user.getChatRoom().getChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                c.writeAndFlush("[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n');
            else
                c.writeAndFlush("[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + '\n');
        });

    }

    public void getUsers(ChannelHandlerContext ctx) {
        user.getChatRoom().getChatRoomUsers().forEach(p ->
        {
            if (user.getUserName().equals(p))
                ctx.writeAndFlush("* " + p + " (** THIS IS YOU)\n");
            else
                ctx.writeAndFlush("* " + p + "\n");
        });
    }

    public String getChatRooms() {
        ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = MANAGER.getChatRoomsMap();
        if (chatRoomConcurrentHashMap.size() == 0)
            return "NO ACTIVE CHAT ROOMS!\n";
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ACTIVE CHATROOMS ARE:\n");
        chatRoomConcurrentHashMap.entrySet()
                .forEach(
                        p -> stringBuffer.append(p.getKey() + " (" + p.getValue().getChannels().size() + ")" + "\n")
                );
        stringBuffer.append("END OF LIST.\n");
        return stringBuffer.toString();
    }
}