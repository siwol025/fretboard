package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.post.domain.Post;
import lombok.Builder;

public record PostDetailResponse(
        Long id,
        String title,
        String content
) {
    @Builder
    public PostDetailResponse(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public static PostDetailResponse of(Post post) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();
    }
}
