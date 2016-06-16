package com.server.handler.web;

import com.server.Main;
import com.server.state.Manager;
import com.server.state.User;
import com.server.util.MessageUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class WebHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    Log LOG = LogFactory.getLog(WebHandler.class);
    static final Manager MANAGER = Manager.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(WebHandler.class);
    final MessageUtil UTIL = new MessageUtil(true, MANAGER);
    User user = null;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new TextWebSocketFrame(
                "WELCOME TO " + InetAddress.getLocalHost().getHostName() + " CHAT SERVER!\n"));
        ctx.writeAndFlush(new TextWebSocketFrame("LOGIN NAME?"));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

        if (!(frame instanceof TextWebSocketFrame)) {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }

        String msg = ((TextWebSocketFrame) frame).text();
        if (msg.startsWith("/") && !(msg.equals("/rooms") || msg.startsWith("/join")
        || msg.startsWith("/user") || msg.equals("/leave") || msg.equals("/quit"))) {
            UTIL.write(ctx, "[SYSTEM] INVALID COMMAND!\n", true);
            return;
        }

        if ("/quit".equals(msg.toLowerCase().trim())) {
            UTIL.write(ctx, "BYE!\n", false);
            ctx.close();
            return;
        }

        if (user == null) {
            msg = msg.toLowerCase().trim();
            if (msg.startsWith("/"))
                UTIL.write(ctx, "[SYSTEM] ENTER LOGIN NAME FIRST!", true);
            else if (msg.trim().length() == 0)
                UTIL.write(ctx, "[SYSTEM] LOGIN NAME CANNOT BE EMPTY!", true);
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
            UTIL.joinChatRoom(this, ctx, msg);
            return;
        }

        if (user.getChatRoom() == null) {
            UTIL.write(ctx, "[SYSTEM] SELECT CHAT ROOM FIRST!\n", true);
            return;
        }

        if (msg.toLowerCase().trim().equals("/leave")) {
            UTIL.leaveChatRoom(user, ctx);
            return;
        }

        if (msg.toLowerCase().trim().startsWith("/user")) {
            String[] data = msg.split(" ", 3);
            if (data.length != 3)
                return;
            UTIL.messageUser(user, data[1], ctx, data[2]);
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
            user = null;
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush("[SYSTEM] CLOSING CHANNEL DUE TO " + Main.CHANNEL_CLOSE_MINUTES + " MINUTES OF INACTIVITY");
                ctx.close();
            }
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


}

