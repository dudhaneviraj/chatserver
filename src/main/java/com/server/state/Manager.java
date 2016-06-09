package com.server.state;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ConcurrentSet;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {

    ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = new ConcurrentHashMap<>();

    ConcurrentSet<String> userConcurrentSet =new ConcurrentSet<>();

    private static Manager MANAGER=new Manager();

    private Manager()
    {}
    public static Manager getInstance(){
        return MANAGER;
    }

    public ChatRoom joinChatRoom(String username,ChannelHandlerContext ctx, String name,boolean isWeb)
    {
        ChatRoom chatRoom=null;
        if(chatRoomConcurrentHashMap.containsKey(name))
            chatRoom = chatRoomConcurrentHashMap.get(name);
        else
            chatRoom=new ChatRoom(name);
        chatRoomConcurrentHashMap.put(name,chatRoom);
        chatRoom.addUser(username,ctx.channel(),isWeb);
        return chatRoom;
    }

    public ConcurrentHashMap<String, ChatRoom> getChatRoomsMap()
    {
        return chatRoomConcurrentHashMap;
    }

    public boolean userExists(String name)
    {
        if(userConcurrentSet.contains(name))
            return true;
        return false;
    }

    public void addUser(String name)
    {
        userConcurrentSet.add(name);
    }

    public void removeUser(String name)
    {
        userConcurrentSet.remove(name);
    }
}
