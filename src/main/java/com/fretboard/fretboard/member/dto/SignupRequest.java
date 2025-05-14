package com.fretboard.fretboard.member.dto;

import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotBlank
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
