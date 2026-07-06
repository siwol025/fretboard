package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.dto.PostSearchSummaryDto;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostSearchListResponse(
        List<PostSearchSummaryDto> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean isFirst,
        boolean isLast
) {
    public static PostSearchListResponse of(Page<PostSearchSummaryDto> page) {
        return new PostSearchListResponse(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );
    }
}
