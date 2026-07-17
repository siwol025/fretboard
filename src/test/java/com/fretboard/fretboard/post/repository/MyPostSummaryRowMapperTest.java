package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.dto.MyPostSummaryDto;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MyPostSummaryRowMapper} 의 위치 기반 매핑 계약을 코드로 고정한다.
 * SELECT 컬럼 순서 {@code (id, title, author, created_at, view_count, board_id, board_title)} 가
 * 그대로 {@link MyPostSummaryDto} 필드로 결합되는지 검증한다.
 */
class MyPostSummaryRowMapperTest {

    @Test
    void Object_7필드를_MyPostSummaryDto로_위치_매핑() {
        // given — JDBC 가 돌려줄 수 있는 형태(Long, Timestamp)로 7컬럼 구성
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 30, 45);
        Object[] row = {
                1L,                              // id
                "제목",                           // title
                "작성자",                         // author(nickname)
                Timestamp.valueOf(createdAt),    // created_at
                42L,                             // view_count
                7L,                              // board_id
                "게시판제목"                       // board_title
        };

        // when
        MyPostSummaryDto dto = MyPostSummaryRowMapper.map(row);

        // then — 각 필드가 올바른 위치로 매핑되고 Timestamp -> LocalDateTime 변환됨
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("제목");
        assertThat(dto.author()).isEqualTo("작성자");
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.viewCount()).isEqualTo(42L);
        assertThat(dto.boardId()).isEqualTo(7L);
        assertThat(dto.boardTitle()).isEqualTo("게시판제목");
    }

    @Test
    void null_createdAt도_안전하게_매핑() {
        // given — created_at 자리에 null
        Object[] row = {1L, "제목", "작성자", null, 0L, 7L, "게시판제목"};

        // when
        MyPostSummaryDto dto = MyPostSummaryRowMapper.map(row);

        // then — NativeRowMappers.toLocalDateTime 의 null 처리로 createdAt 이 null
        assertThat(dto.createdAt()).isNull();
    }
}
