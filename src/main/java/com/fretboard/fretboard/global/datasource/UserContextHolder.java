package com.fretboard.fretboard.global.datasource;

public final class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
