package com.employeeportal.securityapi.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(Exception exception) {
        ErrorResponse errorResponse;

        if (exception instanceof BadCredentialsException) {
            errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        if (exception instanceof AccessDeniedException) {
            errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "You don't have permission to access this resource");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        if (exception instanceof SignatureException) {
            errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "JWT signature is invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        if (exception instanceof ExpiredJwtException) {
            errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "JWT token has expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        if (exception instanceof RuntimeException && exception.getMessage().contains("already exists")) {
            errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
