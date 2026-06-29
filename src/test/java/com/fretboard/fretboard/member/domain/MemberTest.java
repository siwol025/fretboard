package com.fretboard.fretboard.member.domain;

import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    private final PasswordEncryptor encryptor = new PasswordEncryptor();

    @Test
    @DisplayName("Member_matchPassword_일치시_true_반환")
    void matchPassword_withCorrectRawPassword_returnsTrue() {
        // given
        String rawPassword = "correctRawPwd";
        String encodedPassword = encryptor.encode(rawPassword);

        Member member = Member.builder()
                .username("testuser")
                .password(encodedPassword)
                .nickname("testnick")
                .role(Role.USER)
                .build();

        // when & then
        assertTrue(member.matchPassword(rawPassword, encryptor));
    }

    @Test
    @DisplayName("Member_matchPassword_불일치시_false_반환")
    void matchPassword_withWrongRawPassword_returnsFalse() {
        // given
        String rawPassword = "correctRawPwd";
        String encodedPassword = encryptor.encode(rawPassword);

        Member member = Member.builder()
                .username("testuser")
                .password(encodedPassword)
                .nickname("testnick")
                .role(Role.USER)
                .build();

        // when & then
        assertFalse(member.matchPassword("wrongRawPwd", encryptor));
    }
}
