package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.board.domain.PostBoard;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostResponse(
        List<PostListResponse> posts,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean isFirst,
        boolean isLast
) {
    public static PostResponse of(Page<PostBoard> page) {
        List<PostListResponse> content = page.getContent().stream()
                .map(PostBoard::getPost)
                .map(PostListResponse::of)
                .toList();

        return new PostResponse(
                content,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );
    }
}