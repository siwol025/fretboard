package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.domain.Post;
import lombok.Builder;

public record PostSummaryResponse(
        Long id,
        String title,
        int commentCount
) {
    @Builder
    public PostSummaryResponse(Long id, String title, int commentCount) {
        this.id = id;
        this.title = title;
        this.commentCount = commentCount;
    }

    public static PostSummaryResponse of(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .commentCount(post.getComments().size())
                .build();
    }
}
