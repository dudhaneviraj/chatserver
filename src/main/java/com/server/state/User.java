package com.server.state;

import io.netty.channel.ChannelId;

public class User {

    String userName=null;

    ChatRoom chatRoom;

    public User(String userName) {
        this.userName=userName;
    }

    public String getUserName() {
        return userName;
    }

    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    public void addChatRoom(ChatRoom chatRoom) {this.chatRoom=chatRoom;}

    public void leaveChatRoom()
    {
        chatRoom=null;
    }
}
