package com.fretboard.fretboard.member.dto;

import com.fretboard.fretboard.member.domain.Member;
import lombok.Builder;

@Builder
public record MemberEditResponse(
        String newNickname
) {
}
