package com.fretboard.fretboard.post.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * native query {@code Object[]} 행 매핑에서 공통으로 쓰이는 값 변환 유틸.
 *
 * <p>JDBC 드라이버가 시각 컬럼을 {@link Timestamp} 로 돌려주는 경우와
 * {@link LocalDateTime} 으로 돌려주는 경우를 모두 흡수한다.
 */
final class NativeRowMappers {

    private NativeRowMappers() {
    }

    static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
