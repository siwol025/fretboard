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
        Long authorId,
        String author,
        LocalDateTime createdAt,
        Long viewCount,
        Long boardId,
        String boardTitle,
        CommentResponse commentResponse
) {
    public static PostDetailResponse of(Post post, Long viewCount) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getMember().getId())
                .author(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .viewCount(viewCount)
                .commentResponse(CommentResponse.createByComments(post.getComments()))
                .boardId(post.getBoard().getId())
                .boardTitle(post.getBoard().getTitle())
                .build();
    }
}
