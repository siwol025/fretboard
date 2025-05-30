package com.fretboard.fretboard.post.dto;

import java.time.LocalDateTime;

public record PostWithCommentCountDto(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        Long viewCount,
        Long commentCount
) {}
