package com.fretboard.fretboard.global.exception;

import org.springframework.http.HttpStatus;

public record FretBoardErrorResponse(HttpStatus httpStatus, String message) {
    public static FretBoardErrorResponse from(FretBoardException e) {
        return new FretBoardErrorResponse(e.getHttpStatus(), e.getMessage());
    }
}
