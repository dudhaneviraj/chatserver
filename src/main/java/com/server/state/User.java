package com.server.state;

import io.netty.channel.ChannelId;

public class User {

    String userName=null;

    ChatRoom chatRoom;

    ChannelId channelId;

    public User(String userName) {
        this.userName=userName;
    }

    public User(String userName,ChannelId channelId) {
        this.userName = userName;
        this.channelId=channelId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User))
            return false;
        User user = (User) obj;
        if (this.getUserName().equals(user.getUserName()))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
//        hashCode = 5 * hashCode + channelId.hashCode();
        hashCode = 5 * hashCode + ((userName == null)? 0 :userName.hashCode());
        return hashCode;
    }

    public String getUserName() {
        return userName;
    }

    public void addChatRoom(ChatRoom chatRoom) {this.chatRoom=chatRoom;}

    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom=chatRoom;
    }

    public void leaveChatRoom()
    {
        chatRoom=null;
    }
}
