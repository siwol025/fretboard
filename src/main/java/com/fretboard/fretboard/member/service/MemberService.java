package com.fretboard.fretboard.member.service;

import com.fretboard.fretboard.exception.ExceptionType;
import com.fretboard.fretboard.exception.FretBoardException;
import com.fretboard.fretboard.member.dto.SignupRequest;
import com.fretboard.fretboard.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public void signup(final SignupRequest signupRequest) {
        validate(signupRequest);
        memberRepository.save(signupRequest.toMember());
    }

    public void validate(final SignupRequest signupRequest) {
        validateUniqueUsername(signupRequest.username());
        validateUniqueNickname(signupRequest.nickname());
    }

    public void validateUniqueUsername(final String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new FretBoardException(ExceptionType.DUPLICATION_USERNAME);
        }
    }

    public void validateUniqueNickname(final String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new FretBoardException(ExceptionType.DUPLICATION_NICKNAME);
        }
    }
}
