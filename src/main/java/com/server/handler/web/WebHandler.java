package com.server.handler.web;

import java.net.InetAddress;

import com.server.state.Manager;
import com.server.state.User;
import com.server.util.MessageUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    static final Manager MANAGER = Manager.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(WebHandler.class);

    User user = null;

    final MessageUtil UTIL = new MessageUtil(true, MANAGER);


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(new TextWebSocketFrame(
                    "WELCOME TO " + InetAddress.getLocalHost().getHostName() + " CHAT SERVER!\n"));
            ctx.writeAndFlush(new TextWebSocketFrame("LOGIN NAME?"));
   }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled

        if (!(frame instanceof TextWebSocketFrame)) {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
        // Send the uppercase string back.

        String msg = ((TextWebSocketFrame) frame).text();


        if (user == null) {
            msg=msg.toLowerCase().trim();
            if(msg.equals("/rooms") || msg.startsWith("/join") || msg.equals("/leave") || msg.equals("/quit"))
                UTIL.write(ctx,"ENTER LOGIN NAME FIRST!",true);
            else if(msg.trim().length()==0)
                UTIL.write(ctx,"LOGIN NAME CANNOT BE EMPTY!",true);
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
            UTIL.write(ctx, "BYE!\n",true);
            ctx.close();
            return;
        }

        if (user.getChatRoom() == null) {
            UTIL.write(ctx, "SELECT CHAT ROOM FIRST!\n",true);
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

