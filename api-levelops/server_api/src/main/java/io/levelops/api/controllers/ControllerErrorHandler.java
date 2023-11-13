package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.ConnectException;
import java.sql.SQLException;
import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

@Log4j2
@ControllerAdvice
public class ControllerErrorHandler {

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Object> handleInvalidArgumentException(SQLException e) {
        ErrorMessage errorMessage = createErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(), "Unhandled exception during API invocation");
        log.error("SQL exception occured. Error: ", e);
        return new ResponseEntity<>(errorMessage, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<Object> handleConnectException(ConnectException e) {
        ErrorMessage errorMessage = createErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(), "Unhandled exception during API invocation");
        log.error("Connect exception occured. Error: ", e);
        return new ResponseEntity<>(errorMessage, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorMessage createErrorMessage(int status, String error, String message) {
        return ErrorMessage.builder()
                .timestamp(Instant.now().toEpochMilli())
                .status(status)
                .error(error)
                .message(message).build();
    }

    @Value
    @Builder
    @JsonInclude(NON_ABSENT)
    public static class ErrorMessage {
        String error;
        int status;
        String message;
        long timestamp;
    }
}
