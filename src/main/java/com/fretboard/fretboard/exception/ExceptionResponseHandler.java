package com.fretboard.fretboard.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionResponseHandler {

    @ExceptionHandler(FretBoardException.class)
    public ResponseEntity<FretBoardErrorResponse> handleFretBoardException(FretBoardException e) {
        log.warn("FretBoard Custom exception [statusCode = {}, errorMessage = {}, cause = {}]", e.getHttpStatus(), e.getMessage(), e.getStackTrace());
        ResponseEntity.BodyBuilder status = ResponseEntity.status(e.getHttpStatus());
        return status.body(FretBoardErrorResponse.from(e));
    }
}
