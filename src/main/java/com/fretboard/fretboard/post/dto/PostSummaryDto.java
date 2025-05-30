package com.fretboard.fretboard.post.dto;

import java.time.LocalDateTime;

public record PostSummaryDto(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        Long viewCount
) {}
