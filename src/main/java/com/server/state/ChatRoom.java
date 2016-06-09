package com.server.state;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;

public class ChatRoom {

    private String name;

    final ChannelGroup webChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    final ChannelGroup tcpChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    final ConcurrentSet<String> userSet = new ConcurrentSet<>();

    public ChatRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ChannelGroup getWebChannels() {
        return webChannelGroup;
    }

    public ChannelGroup getTCPChannels() {
        return tcpChannelGroup;
    }

    public void addUser(String username, Channel channel,boolean isWeb) {
        if (!userSet.contains(username)) {
            userSet.add(username);
            if(isWeb)
                webChannelGroup.add(channel);
            else
                tcpChannelGroup.add(channel);
        }
    }


    public void removeUser(String username, Channel channel,boolean isWeb) {
        userSet.remove(username);
        if(isWeb)
            webChannelGroup.remove(channel);
        else
            tcpChannelGroup.remove(channel);
    }

    public ConcurrentSet<String> getChatRoomUsers() {
        return userSet;
    }
}
