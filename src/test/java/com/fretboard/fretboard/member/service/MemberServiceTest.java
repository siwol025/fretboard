package com.fretboard.fretboard.member.service;

import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.member.dto.PasswordEditRequest;
import com.fretboard.fretboard.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private final PasswordEncryptor passwordEncryptor = new PasswordEncryptor();

    private MemberService memberService;

    private final Long MEMBER_ID = 1L;
    private final String RAW_PASSWORD = "correctPwd1";

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, passwordEncryptor);
    }

    @Test
    void MemberService_updatePassword_현재비밀번호불일치시_예외() {
        // given
        String bcryptEncoded = passwordEncryptor.encode(RAW_PASSWORD);
        Member member = Member.builder()
                .username("testuser")
                .password(bcryptEncoded)
                .nickname("testnick")
                .role(Role.USER)
                .build();

        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MemberAuth memberAuth = new MemberAuth(MEMBER_ID);
        PasswordEditRequest request = new PasswordEditRequest("wrongPwd1", "newPassword1");

        // when & then
        assertThatThrownBy(() -> memberService.updatePassword(memberAuth, request))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    org.assertj.core.api.Assertions.assertThat(fbe.getExceptionType())
                            .isEqualTo(ExceptionType.INVALID_CURRENT_PASSWORD);
                });
    }

    @Test
    void MemberService_updatePassword_올바른비밀번호_성공() {
        // given
        // BCrypt로 인코딩된 비밀번호를 가진 Member
        String bcryptEncoded = passwordEncryptor.encode(RAW_PASSWORD);
        Member member = Member.builder()
                .username("testuser")
                .password(bcryptEncoded)
                .nickname("testnick")
                .role(Role.USER)
                .build();

        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MemberAuth memberAuth = new MemberAuth(MEMBER_ID);
        PasswordEditRequest request = new PasswordEditRequest(RAW_PASSWORD, "newPassword1");

        // when & then
        // 현재 SHA-512 흐름은 BCrypt 해시를 올바르게 검증하지 못하므로 이 테스트가 FAIL해야 함 (RED)
        assertThatCode(() -> memberService.updatePassword(memberAuth, request))
                .doesNotThrowAnyException();
    }
}
