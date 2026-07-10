package com.fretboard.fretboard.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostEditRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        @Size(max = 10_000, message = "본문은 10,000자를 초과할 수 없습니다.")
        String content
) {

}