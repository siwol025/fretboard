package com.fretboard.fretboard.global.utils;

import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationHelper {

    private final MemberRepository memberRepository;

    public Member getMember(final MemberAuth memberAuth) {
        return memberRepository.findById(memberAuth.memberId())
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
    }

    public void validateIsAuthor(Member author, Member loginMember) {
        boolean isSamePerson = (author == loginMember)
                || (author.getId() != null && author.getId().equals(loginMember.getId()));
        if (!isSamePerson) {
            throw new FretBoardException(ExceptionType.NOT_AUTHOR);
        }
    }
}
