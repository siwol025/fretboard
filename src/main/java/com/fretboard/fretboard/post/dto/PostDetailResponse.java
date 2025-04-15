package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.comment.dto.CommentDetailResponse;
import com.fretboard.fretboard.comment.dto.CommentResponse;
import com.fretboard.fretboard.post.domain.Post;
import java.util.List;
import lombok.Builder;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        CommentResponse commentResponse
) {
    @Builder
    public PostDetailResponse(Long id, String title, String content, CommentResponse commentResponse) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.commentResponse = commentResponse;
    }

    public static PostDetailResponse of(Post post) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .commentResponse(CommentResponse.createByComments(post.getComments()))
                .build();
    }
}
