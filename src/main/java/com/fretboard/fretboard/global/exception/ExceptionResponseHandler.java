package com.fretboard.fretboard.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionResponseHandler {

    @ExceptionHandler(FretBoardException.class)
    public ResponseEntity<FretBoardErrorResponse> handleFretBoardException(FretBoardException e) {
        log.warn("FretBoard Custom exception [statusCode = {}, errorMessage = {}, cause = {}]", e.getHttpStatus(), e.getMessage(), e.getStackTrace());
        return ResponseEntity.status(e.getHttpStatus()).body(FretBoardErrorResponse.from(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FretBoardErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        FretBoardErrorResponse response = new FretBoardErrorResponse(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<FretBoardErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(e.getMessage());
        FretBoardErrorResponse response = new FretBoardErrorResponse(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
