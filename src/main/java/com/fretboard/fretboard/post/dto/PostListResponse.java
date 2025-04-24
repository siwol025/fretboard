package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.post.domain.Post;
import lombok.Builder;

public record PostListResponse(
        Long id,
        String title,
        int commentCount
) {
    @Builder
    public PostListResponse(Long id, String title, int commentCount) {
        this.id = id;
        this.title = title;
        this.commentCount = commentCount;
    }

    public static PostListResponse of(Post post) {
        return PostListResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .commentCount(post.getComments().size())
                .build();
    }
}
