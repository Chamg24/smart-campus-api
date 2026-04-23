package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String field;
    private final String value;

    public LinkedResourceNotFoundException(String field, String value) {
        super("Referenced resource not found: " + field + " = " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }

    @Provider
    public static class Mapper implements ExceptionMapper<LinkedResourceNotFoundException> {
        @Override
        public Response toResponse(LinkedResourceNotFoundException ex) {
            return Response
                    .status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of(
                            "status",  422,
                            "error",   "Unprocessable Entity",
                            "message", "The field '" + ex.getField() + "' references " +
                                       "an id '" + ex.getValue() + "' that does not exist.",
                            "field",   ex.getField()
                    ))
                    .build();
        }
    }
}