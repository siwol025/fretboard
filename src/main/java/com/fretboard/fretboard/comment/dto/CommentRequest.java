package com.fretboard.fretboard.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank
        String content
) {
}
