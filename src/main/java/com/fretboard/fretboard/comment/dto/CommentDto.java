package com.fretboard.fretboard.comment.dto;

public record CommentDto(
        Long id,
        String content,
        Long postId,
        Long authorId  // 작성자 이름 추가
) {}
