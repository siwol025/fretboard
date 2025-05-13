package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.domain.Post;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostListResponse(
        List<PostSummaryResponse> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean isFirst,
        boolean isLast
) {
    public static PostListResponse of(Page<Post> page) {
        List<PostSummaryResponse> content = page.getContent().stream()
                .map(PostSummaryResponse::of)
                .toList();

        return new PostListResponse(
                content,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );
    }
}