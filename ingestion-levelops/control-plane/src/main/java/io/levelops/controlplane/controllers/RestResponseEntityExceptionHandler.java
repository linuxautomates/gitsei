package io.levelops.controlplane.controllers;

import io.levelops.commons.models.ExceptionPrintout;
import org.postgresql.util.PSQLException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {PSQLException.class})
    protected ResponseEntity<Object> handlePSQLException(RuntimeException ex, WebRequest request) {
        return new ResponseEntity<>(Map.of(
                "timestamp", System.currentTimeMillis(),
                "status", 500,
                "error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "exception", ExceptionPrintout.fromThrowable(ex),
                "path", request.getContextPath()
        ), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
