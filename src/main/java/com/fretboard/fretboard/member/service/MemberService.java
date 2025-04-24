package com.fretboard.fretboard.member.service;

import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
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
    private final PasswordEncryptor passwordEncryptor;

    @Transactional
    public void signup(final SignupRequest signupRequest) {
        validate(signupRequest);
        String encryptedPassword = passwordEncryptor.encrypt(signupRequest.password());
        Member member = signupRequest.toMember(encryptedPassword);
        memberRepository.save(member);
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
