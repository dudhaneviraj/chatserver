package com.server.state;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;

import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    final ChannelGroup webChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    final ChannelGroup tcpChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    final ConcurrentSet<String> userSet = new ConcurrentSet<>();
    final ConcurrentHashMap<String,ChannelId> userMap=new ConcurrentHashMap<>();

    private String name;

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

    public ChannelId getChannelId(String username)
    {
        return userMap.get(username);
    }
    public void addUser(String username, Channel channel, boolean isWeb) {
        if (!userSet.contains(username)) {
            userSet.add(username);
            userMap.put(username,channel.id());
            if (isWeb)
                webChannelGroup.add(channel);
            else
                tcpChannelGroup.add(channel);
        }
    }

    public void removeUser(String username, Channel channel, boolean isWeb) {
        userSet.remove(username);
        userMap.remove(username);
        if (isWeb)
            webChannelGroup.remove(channel);
        else
            tcpChannelGroup.remove(channel);
    }

    public ConcurrentHashMap.KeySetView<String, ChannelId> getChatRoomUsers() {
        return userMap.keySet();
    }
}
