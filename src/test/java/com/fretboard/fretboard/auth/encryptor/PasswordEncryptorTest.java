package com.fretboard.fretboard.auth.encryptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncryptorTest {

    private final PasswordEncryptor passwordEncryptor = new PasswordEncryptor();

    @Test
    void PasswordEncryptor_encode_결과는_BCrypt_형식이다() {
        String encoded = passwordEncryptor.encode("password");

        assertThat(encoded).matches("^(\\$2a\\$|\\$2b\\$).*");
        assertThat(encoded).hasSize(60);
    }

    @Test
    void PasswordEncryptor_matches_올바른_raw비밀번호_true_반환() {
        String encoded = passwordEncryptor.encode("myPassword");

        boolean result = passwordEncryptor.matches("myPassword", encoded);

        assertThat(result).isTrue();
    }

    @Test
    void PasswordEncryptor_matches_잘못된_raw비밀번호_false_반환() {
        String encoded = passwordEncryptor.encode("correctPassword");

        boolean result = passwordEncryptor.matches("wrongPassword", encoded);

        assertThat(result).isFalse();
    }
}
