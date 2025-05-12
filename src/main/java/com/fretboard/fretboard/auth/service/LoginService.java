package com.fretboard.fretboard.auth.service;

import com.fretboard.fretboard.auth.dto.LoginInfoDto;
import com.fretboard.fretboard.auth.dto.request.LoginRequest;
import com.fretboard.fretboard.auth.dto.request.TokenReissueRequest;
import com.fretboard.fretboard.auth.dto.response.LoginResponse;
import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncryptor passwordEncryptor;

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(() -> new FretBoardException(ExceptionType.INVALID_USERNAME));

        String encryptedPassword = passwordEncryptor.encrypt(request.password());
        validatePassword(member, encryptedPassword);

        return LoginResponse.of(member, jwtTokenProvider.createToken(LoginInfoDto.of(member)));
    }

    public LoginResponse reissueToken(TokenReissueRequest request) {
        String memberId = jwtTokenProvider.decodeRefreshToken(request.reissueToken());
        Member member = memberRepository.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
        return LoginResponse.of(member, jwtTokenProvider.createToken(LoginInfoDto.of(member)));
    }

    private void validatePassword(Member member, String password) {
        if (!member.matchPassword(password)) {
            throw new FretBoardException(ExceptionType.INVALID_PASSWORD);
        }
    }
}
