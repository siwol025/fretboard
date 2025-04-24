package com.fretboard.fretboard.auth.dto.response;

import com.fretboard.fretboard.member.domain.Member;
import lombok.Builder;

@Builder
public record LoginResponse(
        Long memberId,
        String nickname,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse of(Member member, TokenResponse tokenResponse) {
        return LoginResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .accessToken(tokenResponse.accessToken())
                .refreshToken(tokenResponse.refreshToken())
                .build();
    }
}
