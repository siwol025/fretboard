package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.dto.MyPostSummaryDto;

/**
 * 내 게시글 목록 Deferred Join native query 의 {@code Object[]} 결과 행을 {@link MyPostSummaryDto} 로 변환한다.
 *
 * <p>SELECT 컬럼 순서 {@code (id, title, author, created_at, view_count, board_id, board_title)} 에
 * 위치 기반으로 결합된다. Timestamp 변환은 {@link NativeRowMappers} 를 공유한다.
 */
final class MyPostSummaryRowMapper {

    private MyPostSummaryRowMapper() {
    }

    static MyPostSummaryDto map(Object[] row) {
        return new MyPostSummaryDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                NativeRowMappers.toLocalDateTime(row[3]),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                (String) row[6]
        );
    }
}
