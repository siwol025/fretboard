package com.fretboard.fretboard.post.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditPostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        String content
) {

}