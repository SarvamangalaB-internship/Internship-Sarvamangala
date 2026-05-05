package com.urbanvogue.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("error", ex.getMessage());

        // Check if it's an access denied error
        if (ex.getMessage() != null &&
                ex.getMessage().contains("Access Denied")) {
            error.put("status", 403);
            error.put("hint",
                    "Add header: X-User-Role = ROLE_ADMIN");
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(error);
        }

        error.put("status", 400);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
}