package com.server.handler.tcp;

import com.server.state.Manager;
import com.server.state.User;
import com.server.util.MessageUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;


public class TCPHandler extends SimpleChannelInboundHandler<String> {


    static final Manager MANAGER = new Manager();

    User user = null;

    final MessageUtil UTIL = new MessageUtil(false, MANAGER);


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        UTIL.write(ctx,"WELCOME TO " + InetAddress.getLocalHost().getHostName() + " CHAT SERVER!");
        UTIL.write(ctx,"LOGIN NAME:");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        if (user == null) {
            msg = msg.toLowerCase().trim();
            if (msg.equals("/rooms") || msg.startsWith("/join") || msg.equals("/leave"))
                UTIL.write(ctx, "ENTER LOGIN NAME FIRST!");
            else if(msg.trim().length()==0)
                UTIL.write(ctx,"LOGIN NAME CANNOT BE EMPTY!");
            else
                UTIL.firstLogin(this, ctx, msg);
            return;
        }

        if (msg.toLowerCase().trim().equals("/rooms")) {
            UTIL.getChatRooms(ctx);
            return;
        }

        if (msg.toLowerCase().trim().startsWith("/join") && !msg.toLowerCase().trim().endsWith("/join")) {
            UTIL.leaveChatRoom(user, ctx);
//            leaveChatRoom(ctx);
            UTIL.joinChatRoom(this, ctx, msg);
            return;
        }

        if ("/quit".equals(msg.toLowerCase().trim())) {
            UTIL.leaveChatRoom(user, ctx);
            MANAGER.removeUser(user.getUserName());
            UTIL.write(ctx, "BYE!\n");
            ctx.close();
            return;
        }

        if (user.getChatRoom() == null) {
            UTIL.write(ctx, "SELECT CHAT ROOM FIRST!\n");
            return;
        }

        if (msg.toLowerCase().trim().equals("/leave")) {
            UTIL.leaveChatRoom(user, ctx);
            return;
        }

        UTIL.broadCast(user, ctx, msg);
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
            UTIL.leaveChatRoom(user, ctx);
            MANAGER.removeUser(user.getUserName());
        }
        ctx.fireChannelInactive();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}