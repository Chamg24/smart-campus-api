package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version",     "1.0.0");
        response.put("description", "Smart Campus Sensor & Room Management API");
        response.put("contact",     "admin@westminster.ac.uk");
        response.put("status",      "operational");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);

        return Response.ok(response).build();
    }
    
        @GET
    @Path("/crash")
    public Response crash() {
        String s = null;
        s.length(); 
        return Response.ok().build();
    }


}