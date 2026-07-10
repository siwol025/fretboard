package com.fretboard.fretboard.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "댓글을 입력해주세요.")
        @Size(max = 1_000, message = "댓글은 1,000자를 초과할 수 없습니다.")
        String content
) {
}
