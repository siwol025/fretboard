package com.fretboard.fretboard.post.dto;

import java.time.LocalDateTime;

public record PostSearchSummaryDto(
        Long id,
        String title,
        String author,
        Long boardId,
        String boardTitle,
        LocalDateTime createdAt,
        Long viewCount,
        Long commentCount
) {}
