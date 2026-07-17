package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.dto.PostSummaryDto;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PostSummaryRowMapper} 의 위치 기반 매핑 계약을 코드로 고정한다.
 * SELECT 컬럼 순서 {@code (id, title, author, created_at, view_count)} 가
 * 그대로 {@link PostSummaryDto} 필드로 결합되는지 검증한다.
 */
class PostSummaryRowMapperTest {

    @Test
    void Object_5필드를_PostSummaryDto로_위치_매핑() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 30, 45);
        Object[] row = {
                1L,                              // id
                "제목",                           // title
                "작성자",                         // author
                Timestamp.valueOf(createdAt),    // created_at
                42L                              // view_count
        };

        // when
        PostSummaryDto dto = PostSummaryRowMapper.map(row);

        // then
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("제목");
        assertThat(dto.author()).isEqualTo("작성자");
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.viewCount()).isEqualTo(42L);
    }

    @Test
    void null_createdAt도_안전하게_매핑() {
        // given — created_at 자리에 null
        Object[] row = {1L, "제목", "작성자", null, 0L};

        // when
        PostSummaryDto dto = PostSummaryRowMapper.map(row);

        // then
        assertThat(dto.createdAt()).isNull();
    }
}
