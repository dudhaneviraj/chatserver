package com.server.state;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;

import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    private String name;


    final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    final ConcurrentSet<String> userSet=new ConcurrentSet<>();

    public ChatRoom(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    public ChannelGroup getChannels() {
        return channelGroup;
    }


    public void addUser(String username,Channel channel) {
        if(!userSet.contains(username)) {
            userSet.add(username);
            channelGroup.add(channel);
        }
    }

    public void removeUser(String username,Channel channel) {
        userSet.remove(username);
        channelGroup.remove(channel);
    }


    public ConcurrentSet<String> getChatRoomUsers()
    {
        return userSet;
    }
}
