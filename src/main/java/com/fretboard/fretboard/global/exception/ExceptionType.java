package com.fretboard.fretboard.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionType {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시물을 찾을 수 없습니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시판을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    S3_NOT_FOUND(HttpStatus.NOT_FOUND, "S3에 해당 이미지가 존재하지 않습니다."),

    DUPLICATION_USERNAME(HttpStatus.BAD_REQUEST, "중복된 아이디입니다."),
    DUPLICATION_NICKNAME(HttpStatus.BAD_REQUEST, "중복된 닉네임입니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NOT_AUTHOR(HttpStatus.BAD_REQUEST, "글 작성자가 아닙니다."),

    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    S3_UPLOAD_EXCEPTION(HttpStatus.BAD_REQUEST, "S3에 이미지 업로드 중 오류가 발생했습니다."),
    S3_FORMAT_EXCEPTION(HttpStatus.BAD_REQUEST, "S3 이미지 url 형식이 잘못되었습니다."),
    S3_DELETE_EXCEPTION(HttpStatus.BAD_REQUEST, "S3에서 이미지 삭제 중 오류가 발생했습니다."),

    FORBIDDEN_WRITE_PERMISSION(HttpStatus.FORBIDDEN, "글쓰기 권한이 없습니다.");
    private final HttpStatus httpStatus;
    private final String message;

    ExceptionType(final HttpStatus httpStatus, final String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
