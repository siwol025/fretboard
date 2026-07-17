package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.dto.PostSummaryDto;

/**
 * Deferred Join native query 의 {@code Object[]} 결과 행을 {@link PostSummaryDto} 로 변환한다.
 *
 * <p>SELECT 컬럼 순서 {@code (id, title, author, created_at, view_count)} 에 위치 기반으로 결합되므로,
 * 이 순서를 그대로 사용하는 모든 native 목록 쿼리(게시판 목록·내 게시글 등)가 이 매퍼를 공유한다.
 * 컬럼 순서를 바꾸면 이 매퍼 한 곳만 수정하면 된다.
 */
final class PostSummaryRowMapper {

    private PostSummaryRowMapper() {
    }

    static PostSummaryDto map(Object[] row) {
        return new PostSummaryDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                NativeRowMappers.toLocalDateTime(row[3]),
                ((Number) row[4]).longValue()
        );
    }
}
