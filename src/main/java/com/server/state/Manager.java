package com.server.state;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.util.internal.ConcurrentSet;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {

    ConcurrentHashMap<String, ChatRoom> chatRoomConcurrentHashMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, User> userConcurrentHashMap = new ConcurrentHashMap<>();

    ConcurrentSet<String> userConcurrentSet =new ConcurrentSet<>();

    private static Manager manager = new Manager();

    private Manager() {}

    public static Manager getInstance(){
        return manager;
    }

    public ChatRoom createChatRoom(String name)
    {
            if(chatRoomConcurrentHashMap.containsKey(name))
                return chatRoomConcurrentHashMap.get(name);
            ChatRoom chatRoom=new ChatRoom(name);
            chatRoomConcurrentHashMap.put(name,chatRoom);
            return chatRoom;
    }


    public ChatRoom joinChatRoom(User user,ChannelHandlerContext ctx, String name)
    {
        ChatRoom chatRoom=null;
        if(chatRoomConcurrentHashMap.containsKey(name))
            chatRoom = chatRoomConcurrentHashMap.get(name);
        else
            chatRoom=new ChatRoom(name);
        chatRoomConcurrentHashMap.put(name,chatRoom);
        chatRoom.addUser(user.getUserName(),ctx.channel());
        return chatRoom;
    }

    public ConcurrentHashMap<String, ChatRoom> getChatRoomsMap()
    {
        return chatRoomConcurrentHashMap;
    }

    public boolean userExists(String name,ChannelId channelId)
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
