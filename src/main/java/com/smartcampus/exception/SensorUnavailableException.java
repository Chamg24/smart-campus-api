package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor " + sensorId + " is under maintenance");
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }

    @Provider
    public static class Mapper implements ExceptionMapper<SensorUnavailableException> {
        @Override
        public Response toResponse(SensorUnavailableException ex) {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of(
                            "status",   403,
                            "error",    "Sensor Unavailable",
                            "message",  "Sensor '" + ex.getSensorId() + "' is under MAINTENANCE " +
                                        "and cannot accept new readings."
                    ))
                    .build();
        }
    }
}