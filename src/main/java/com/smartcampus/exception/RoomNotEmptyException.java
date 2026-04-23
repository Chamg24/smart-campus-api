package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int    sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room " + roomId + " still has " + sensorCount + " sensor(s)");
        this.roomId      = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId()      { return roomId; }
    public int    getSensorCount() { return sensorCount; }

    @Provider
    public static class Mapper implements ExceptionMapper<RoomNotEmptyException> {
        @Override
        public Response toResponse(RoomNotEmptyException ex) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of(
                            "status",  409,
                            "error",   "Room Not Empty",
                            "message", "Cannot delete room '" + ex.getRoomId() +
                                       "' — it still has " + ex.getSensorCount() +
                                       " sensor(s). Remove all sensors first."
                    ))
                    .build();
        }
    }
}