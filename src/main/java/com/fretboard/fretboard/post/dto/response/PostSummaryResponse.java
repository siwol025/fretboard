package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.domain.Post;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PostSummaryResponse(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        int commentCount
) {
    public static PostSummaryResponse of(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .author(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .commentCount(post.getComments().size())
                .build();
    }
}
