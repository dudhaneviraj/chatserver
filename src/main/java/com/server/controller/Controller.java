package com.server.controller;


import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.server.state.Manager;
import org.json.JSONObject;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Controller {

    @Path("/users/{chatroom}")
    @GET
    public Set<String> fetchUsers(@PathParam("chatroom") String chatroom) {
        if (Manager.getInstance().getChatRoomsMap().containsKey(chatroom))
            return Manager.getInstance().getChatRoomsMap().get(chatroom).getChatRoomUsers();
        return new HashSet<>();
    }

    @Path("/chatRooms")
    @GET
    @Produces("application/json")
    public Map<String, Integer> chatRooms() {
        return Manager.getInstance().getChatRoomsMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().getChatRoomUsers().size()));
    }

}