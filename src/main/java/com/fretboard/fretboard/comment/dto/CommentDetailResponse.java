package com.fretboard.fretboard.comment.dto;

import com.fretboard.fretboard.comment.domain.Comment;
import lombok.Builder;

public record CommentDetailResponse(
        Long id,
        String content
) {
    @Builder
    public CommentDetailResponse(Long id, String content) {
        this.id = id;
        this.content = content;
    }

    public static CommentDetailResponse from(Comment comment) {
        return CommentDetailResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .build();
    }
}
