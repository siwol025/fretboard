package com.fretboard.fretboard.auth.dto;

import com.fretboard.fretboard.member.domain.Member;
import lombok.Builder;

@Builder
public record LoginInfoDto(
        Long memberId,
        String username,
        String nickname
) {
    public static LoginInfoDto of(Member member) {
        return LoginInfoDto.builder()
                .memberId(member.getId())
                .username(member.getUsername())
                .nickname(member.getNickname())
                .build();
    }
}
