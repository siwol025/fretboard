package com.fretboard.fretboard.global.utils;

import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthorizationHelperTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthorizationHelper authorizationHelper;

    @Test
    void getMember_존재하는멤버_반환() {
        Long memberId = 1L;
        MemberAuth memberAuth = new MemberAuth(memberId);
        Member member = Member.builder()
                .username("testuser")
                .password("encoded_password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        Member result = authorizationHelper.getMember(memberAuth);

        assertThat(result).isEqualTo(member);
    }

    @Test
    void getMember_없는멤버_MEMBER_NOT_FOUND_예외() {
        Long memberId = 999L;
        MemberAuth memberAuth = new MemberAuth(memberId);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationHelper.getMember(memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.MEMBER_NOT_FOUND);
                });
    }

    @Test
    void validateIsAuthor_같은객체_예외없음() {
        Member member = Member.builder()
                .username("testuser")
                .password("encoded_password")
                .nickname("testnick")
                .role(Role.USER)
                .build();

        assertDoesNotThrow(() -> authorizationHelper.validateIsAuthor(member, member));
    }

    @Test
    void validateIsAuthor_같은id_다른객체_예외없음() {
        Member author = new Member(1L, "author", "encoded_password", "authornick", Role.USER);
        Member loginMember = new Member(1L, "author", "encoded_password", "authornick", Role.USER);

        assertDoesNotThrow(() -> authorizationHelper.validateIsAuthor(author, loginMember));
    }

    @Test
    void validateIsAuthor_다른멤버_NOT_AUTHOR_예외() {
        Member author = new Member(1L, "author", "encoded_password", "authornick", Role.USER);
        Member other = new Member(2L, "other", "encoded_password", "othernick", Role.USER);

        assertThatThrownBy(() -> authorizationHelper.validateIsAuthor(author, other))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.NOT_AUTHOR);
                });
    }
}
