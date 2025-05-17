package com.fretboard.fretboard.comment.dto.response;

import com.fretboard.fretboard.comment.domain.Comment;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CommentDetailResponse(
        Long id,
        String content,
        Long authorId,
        String author,
        LocalDateTime createdAt
) {
    public static CommentDetailResponse from(Comment comment) {
        return CommentDetailResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getMember().getId())
                .author(comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
