package com.fretboard.fretboard.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordEditRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9!@#$%^&*]{8,}$",
                message = "비밀번호는 8자 이상이며, 영문과 숫자를 포함하고 허용된 문자만 사용해야 합니다.")
        String newPassword
) {
}
