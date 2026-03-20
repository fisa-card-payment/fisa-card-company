package dev.bank.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankException.class)
    public ResponseEntity<Map<String, Object>> handleBankException(BankException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "code", e.getCode(),
                "message", e.getMessage()
        ));
    }
}
