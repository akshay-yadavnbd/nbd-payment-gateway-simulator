package com.nbd.payments.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.nbd.payments.merchant.exception.DuplicateMerchantException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice 

public class ApiExceptionHandler {
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException ex
    ){
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.putIfAbsent(
                error.getField(),
                error.getDefaultMessage()
            )
        );

        Map<String, Object> body = Map.of(
                "error","VALIDATION_ERROR",
                "message", "Invalid request data",
                "fields", fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    //Incorect merchant login credentials 

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
        BadCredentialsException ex
    ){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "INVALID_CREDENTIALS",
                    "message", ex.getMessage()
                )
            );

    }

    // Merchant account is not active

    @ExceptionHandler (DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(
        DisabledException ex
    ){
        return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error", "ACCOUNT_NOT_ACTIVE",
                        "message", ex.getMessage()
                    )
                );
    }
    //Duplicate exception handler 
    @ExceptionHandler (DuplicateMerchantException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMerchant(
        DuplicateMerchantException ex
    ){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "error", "DUPLICATE_MERCHANT",
                    "message", ex.getMessage()
                )
            );
    }

    //Other explicitly defined HTTP errors

    @ExceptionHandler (ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
        ResponseStatusException ex
    ){
        HttpStatus status = HttpStatus.valueOf(
            ex.getStatusCode().value()
        );

        return  ResponseEntity
                    .status(status)
                    .body(Map.of(
                        "error", status.name(),
                        "message", ex.getReason() !=null
                                    ? ex.getReason()
                                    : status.getReasonPhrase()
                    )
                );
    }

    
}
