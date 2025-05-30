package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.dto.PostWithCommentCountDto;
import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record PostListResponse(
        List<PostWithCommentCountDto> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean isFirst,
        boolean isLast
) {
    public static PostListResponse of(Page<PostWithCommentCountDto> page) {
        return PostListResponse.builder()
                .posts(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
