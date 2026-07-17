package com.fretboard.fretboard.post.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record MyPostListResponse(
        List<PostSummaryResponse> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean isFirst,
        boolean isLast
) {
    public static MyPostListResponse of(Page<PostSummaryResponse> page) {
        return new MyPostListResponse(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );
    }
}