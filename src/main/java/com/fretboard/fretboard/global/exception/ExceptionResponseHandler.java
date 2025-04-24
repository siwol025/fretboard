package com.fretboard.fretboard.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
}
