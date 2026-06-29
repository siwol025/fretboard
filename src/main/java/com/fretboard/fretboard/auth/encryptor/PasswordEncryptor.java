package com.fretboard.fretboard.auth.encryptor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncryptor {

    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    public String encode(String raw) {
        return bcryptEncoder.encode(raw);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return bcryptEncoder.matches(rawPassword, encodedPassword);
    }
}
