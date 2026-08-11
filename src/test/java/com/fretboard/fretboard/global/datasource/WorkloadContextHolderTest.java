package com.fretboard.fretboard.global.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadContextHolderTest {

    @AfterEach
    void tearDown() {
        WorkloadContextHolder.clear();
    }

    @Test
    void 기본값은_SERVING() {
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.SERVING);
    }

    @Test
    void set_후_getWorkload_반영() {
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.ANALYTICS);
    }

    @Test
    void clear_후_기본값_SERVING() {
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);
        WorkloadContextHolder.clear();
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.SERVING);
    }
}
