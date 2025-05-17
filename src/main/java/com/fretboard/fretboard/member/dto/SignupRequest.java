package com.fretboard.fretboard.member.dto;

import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]{4,12}$",
                message = "아이디는 4~12자의 영문 또는 숫자만 가능합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9!@#$%^&*]{8,}$",
                message = "비밀번호는 8자 이상이며, 영문과 숫자를 포함하고 허용된 문자만 사용해야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2,max = 15, message = "닉네임은 2자 이상, 15자 이하여야 합니다.")
        String nickname
) {
    public Member toMember(String encryptedPassword) {
        return Member.builder()
                .username(username)
                .password(encryptedPassword)
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }
}
