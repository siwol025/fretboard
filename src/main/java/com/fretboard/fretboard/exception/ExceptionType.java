package com.fretboard.fretboard.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionType {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시물을 찾을 수 없습니다."),
    BOARD_NOT_FOUNT(HttpStatus.NOT_FOUND, "해당 게시판을 찾을 수 없습니다."),
    DUPLICATION_USERNAME(HttpStatus.BAD_REQUEST, "중복된 아이디입니다."),
    DUPLICATION_NICKNAME(HttpStatus.BAD_REQUEST, "중복된 닉네임입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ExceptionType(final HttpStatus httpStatus, final String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
