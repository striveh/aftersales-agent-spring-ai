package dev.striveh.aftersales.agent.api;

import dev.striveh.aftersales.agent.tool.ApprovalService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiError("bad_request", "message 不能为空"));
    }

    @ExceptionHandler(ApprovalService.ApprovalNotFoundException.class)
    public ResponseEntity<ApiError> missing(ApprovalService.ApprovalNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("not_found", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegal(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("bad_request", exception.getMessage()));
    }
}
