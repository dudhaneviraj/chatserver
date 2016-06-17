package com.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.server.handler.web.WebHandler;
import com.server.state.ChatRoom;
import com.server.state.Manager;
import com.server.state.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;


public class WebMessageUtil {

    Manager MANAGER = Manager.getInstance();


    private static WebMessageUtil webMessageUtil = new WebMessageUtil();

    private WebMessageUtil() {
    }

    public static WebMessageUtil getInstance() {
        return webMessageUtil;
    }

    public void write(Channel ctx, String from, String to, String msg) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dataTable = mapper.createObjectNode();
        dataTable.put("from", from);
        dataTable.put("to", to);
        dataTable.put("message", msg);
        ctx.writeAndFlush(new TextWebSocketFrame(dataTable.toString()));
    }

    public void write(ChannelHandlerContext ctx, String from, String to, String msg) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dataTable = mapper.createObjectNode();
        dataTable.put("from", from);
        dataTable.put("to", to);
        dataTable.put("message", msg);
        ctx.writeAndFlush(new TextWebSocketFrame(dataTable.toString()));
    }


    public void firstLogin(WebHandler serverHandler, ChannelHandlerContext ctx, String msg) {

        if (MANAGER.userExists(msg))
            write(ctx, "SYSTEM", "ANONYMOUS", "USERNAME ALREADY IN USE! REENTER LOGIN NAME!");
        else {
            User user = new User(msg);
            serverHandler.setUser(user);
            MANAGER.addUser(user.getUserName());
            write(ctx, "SYSTEM", user.getUserName(), "WELCOME " + user.getUserName() + "!");
        }
    }

    public void joinChatRoom(WebHandler serverHandler, TcpMessageUtil tcpMessageUtil, ChannelHandlerContext ctx, String msg) {
        User user = serverHandler.getUser();
        ChatRoom chatRoom = MANAGER.joinChatRoom(user.getUserName(), ctx, msg.split(" ", 2)[1].trim(), true);
        user.addChatRoom(chatRoom);
        chatRoom.addUser(user.getUserName(), ctx.channel(), true);
        write(ctx, "SYSTEM", user.getUserName(), "ENTERING CHAT ROOM: " + chatRoom.getName());


        broadCastMessage(tcpMessageUtil, user, ctx, "* NEW USER JOINED " + user.getChatRoom().getName() + ": " + user.getUserName());
    }

    public void leaveChatRoom(TcpMessageUtil tcpMessageUtil, User user, ChannelHandlerContext ctx) {
        if (user.getChatRoom() != null) {
            broadCast(tcpMessageUtil, user, ctx, "GOTTA GO!");
            broadCastMessage(tcpMessageUtil, user, ctx, "* USER HAS LEFT CHAT: " + user.getUserName());
            user.getChatRoom().removeUser(user.getUserName(), ctx.channel(), true);
            MANAGER.removeChatRoom(user.getChatRoom().getName());
            user.leaveChatRoom();
        }
    }

    public void broadCastMessage(TcpMessageUtil tcpMessageUtil, User user, ChannelHandlerContext ctx, String msg) {
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, user.getUserName(), user.getChatRoom().getName(), msg);
            else
                write(c, "SYSTEM", user.getChatRoom().getName(), msg + " (** THIS IS YOU)");
        });

        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                tcpMessageUtil.write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
            else
                tcpMessageUtil.write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg + " (** THIS IS YOU)");
        });
    }

    public void broadCast(TcpMessageUtil tcpMessageUtil, User user, ChannelHandlerContext ctx, String msg) {

        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                write(c, user.getUserName(), user.getChatRoom().getName(), msg);
            else
                write(c, "YOU", user.getChatRoom().getName(), msg);
        });


        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c != ctx.channel())
                tcpMessageUtil.write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
            else
                tcpMessageUtil.write(c, "[USER: YOU] " + "[ROOM: " + user.getChatRoom().getName() + "] " + msg);
        });
    }

    public void messageUser(TcpMessageUtil tcpMessageUtil, User user, String username, ChannelHandlerContext ctx, String msg) {
        ChannelId id = user.getChatRoom().getChannelId(username);
        if (id == null) {
            write(ctx, "SYSTEM", user.getUserName(), "NO SUCH USER!");
            return;
        }
        ChannelGroup channelGroup = user.getChatRoom().getWebChannels();
        channelGroup.stream().forEach(c -> {
            if (c.id().asLongText().equals(id.asLongText()) && c != ctx.channel())
                write(c, user.getUserName(), user.getChatRoom().getName(), msg);
        });

        channelGroup = user.getChatRoom().getTCPChannels();

        channelGroup.stream().forEach(c -> {
            if (c.id().asLongText().equals(id.asLongText()) && c != ctx.channel())
                tcpMessageUtil.write(c, "[USER: " + user.getUserName() + "] " + "[ROOM: " + user.getChatRoom().getName() + "] [PERSONAL]" + msg);
        });
        write(ctx, "YOU", user.getUserName(), msg);
    }


}
