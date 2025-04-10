package com.fretboard.fretboard.exception;

public record FretBoardErrorResponse(ExceptionType exceptionType, String message) {
    public static FretBoardErrorResponse from(FretBoardException e) {
        return new FretBoardErrorResponse(e.getExceptionType(), e.getMessage());
    }
}
