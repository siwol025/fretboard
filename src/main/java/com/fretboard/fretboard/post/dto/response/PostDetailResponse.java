package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.post.domain.Post;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        Long viewCount,
        Long boardId,
        String boardTitle,
        CommentResponse commentResponse
) {
    public static PostDetailResponse of(Post post) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .viewCount(post.getViewCount())
                .commentResponse(CommentResponse.createByComments(post.getComments()))
                .boardId(post.getBoard().getId())
                .boardTitle(post.getBoard().getTitle())
                .build();
    }
}
