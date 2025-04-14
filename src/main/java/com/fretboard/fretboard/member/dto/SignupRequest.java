package com.fretboard.fretboard.member.dto;

import com.fretboard.fretboard.member.domain.Member;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotBlank
        String nickname
) {
    public Member toMember() {
        return Member.builder()
                .username(username)
                .password(password)
                .nickname(nickname)
                .build();
    }
}
