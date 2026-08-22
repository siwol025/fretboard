package com.fretboard.fretboard.global.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void set_후_getUserId_반환() {
        UserContextHolder.set(1L);
        assertThat(UserContextHolder.getUserId()).isEqualTo(1L);
    }

    @Test
    void clear_후_getUserId_null() {
        UserContextHolder.set(1L);
        UserContextHolder.clear();
        assertThat(UserContextHolder.getUserId()).isNull();
    }
}
