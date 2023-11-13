package io.levelops.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ServerApiException extends ResponseStatusException {

    /**
     *
     */
    private static final long serialVersionUID = -4650040783094748841L;

    public ServerApiException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public ServerApiException(HttpStatus statusCode, String message) {
        super(statusCode, message);
    }

    public ServerApiException(HttpStatus statusCode, Exception e) {
        super(statusCode, e.getMessage());
    }
}
