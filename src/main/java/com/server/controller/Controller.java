package com.server.controller;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.server.state.Manager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Controller {

    @Path("/users/{chatroom}")
    @GET
    public Set<String> fetchUsers(@PathParam("chatroom") String chatroom) {
        if(Manager.getInstance().getChatRoomsMap().containsKey(chatroom))
            return Manager.getInstance().getChatRoomsMap().get(chatroom).getChatRoomUsers();
        return new HashSet<>();
    }


    @Path("/chatRooms")
    @GET
    @Produces("application/json")
    public Map<String, Integer> chatRooms() {

        return        Manager.getInstance().getChatRoomsMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,x->x.getValue().getChatRoomUsers().size()));

//        return Manager.getInstance().getChatRoomsMap().entrySet().keySet().stream().map(p->).collect(Collectors.toList());
    }

}