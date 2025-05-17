package com.fretboard.fretboard.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberEditRequest(
        @NotBlank
        @Size(min = 2, max = 15, message = "닉네임은 2자 이상, 15자 이하여야 합니다.")
        String newNickname
) {
}
