package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.post.dto.PostStaticView;
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
        CommentResponse commentResponse,
        Long likeCount,
        Boolean isLiked
) {
    public static PostDetailResponse of(
            PostStaticView staticView,
            Long viewCount,
            Long likeCount,
            Boolean isLiked,
            CommentResponse commentResponse) {
        return PostDetailResponse.builder()
                .id(staticView.id())
                .title(staticView.title())
                .content(staticView.content())
                .authorId(staticView.authorId())
                .author(staticView.author())
                .createdAt(staticView.createdAt())
                .viewCount(viewCount)
                .commentResponse(commentResponse)
                .boardId(staticView.boardId())
                .boardTitle(staticView.boardTitle())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}
