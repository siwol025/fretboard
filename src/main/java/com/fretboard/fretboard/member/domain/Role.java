package com.fretboard.fretboard.member.domain;

public enum Role {
    USER,
    ADMIN;

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
