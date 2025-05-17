package com.fretboard.fretboard.member.service;

import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.dto.MemberEditRequest;
import com.fretboard.fretboard.member.dto.MemberEditResponse;
import com.fretboard.fretboard.member.dto.PasswordEditRequest;
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

    @Transactional
    public void updatePassword(final MemberAuth memberAuth, final PasswordEditRequest request) {
        Member member = getMember(memberAuth.memberId());
        String encryptedCurrentPassword = passwordEncryptor.encrypt(request.currentPassword());
        validateCurrentPassword(member, encryptedCurrentPassword);

        String encryptedNewPassword = passwordEncryptor.encrypt(request.newPassword());
        member.setPassword(encryptedNewPassword);
    }

    @Transactional
    public MemberEditResponse updateMemberInfo(final MemberAuth memberAuth, final MemberEditRequest request) {
        Member member = getMember(memberAuth.memberId());
        validateUniqueNickname(request.newNickname());
        member.setNickname(request.newNickname());

        return MemberEditResponse.builder()
                .newNickname(member.getNickname())
                .build();
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

    private Member getMember(final Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
    }

    private void validateCurrentPassword(final Member member, String currentPassword) {
        if (!member.matchPassword(currentPassword)) {
            throw new FretBoardException(ExceptionType.INVALID_CURRENT_PASSWORD);
        }
    }
}
