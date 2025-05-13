package com.fretboard.fretboard.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank
        String content
) {
}
