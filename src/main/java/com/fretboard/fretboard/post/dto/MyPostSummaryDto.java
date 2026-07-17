package com.fretboard.fretboard.post.dto;

import java.time.LocalDateTime;

public record MyPostSummaryDto(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        Long viewCount,
        Long boardId,
        String boardTitle
) {}
