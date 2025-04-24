package com.fretboard.fretboard.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FretBoardException extends RuntimeException{
    private final ExceptionType exceptionType;

    public FretBoardException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }

    public HttpStatus getHttpStatus() {
        return exceptionType.getHttpStatus();
    }
}
