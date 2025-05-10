package com.example.leaveapproval.exception;

import com.example.leaveapproval.dto.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        MessageResponse messageResponse = new MessageResponse(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        // 可以尝试从异常信息中提取更友好的提示，但通常比较困难
        String message = "Data integrity violation. Please check your input.";
        if (ex.getMostSpecificCause().getMessage().contains("Duplicate entry")) {
            message = "Duplicate entry. This username or email might already exist.";
        } else if (ex.getMostSpecificCause().getMessage().contains("ConstraintViolationException")) {
            message = "A database constraint was violated.";
        }
        MessageResponse messageResponse = new MessageResponse(message);
        return new ResponseEntity<>(messageResponse, HttpStatus.CONFLICT); // 409 Conflict
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        MessageResponse messageResponse = new MessageResponse(ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // 处理 @Valid 校验失败
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class) // 处理权限不足 (403)
    public ResponseEntity<MessageResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        MessageResponse messageResponse = new MessageResponse("Access Denied: You do not have permission to perform this action.");
        return new ResponseEntity<>(messageResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class) // 通用异常处理
    public ResponseEntity<MessageResponse> handleGlobalException(Exception ex, WebRequest request) {
        // 记录日志 ex.printStackTrace();
        MessageResponse messageResponse = new MessageResponse("An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(messageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}