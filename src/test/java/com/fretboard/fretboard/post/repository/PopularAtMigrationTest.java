package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V6 마이그레이션이 인기글 조회용 popular_at 컬럼과 복합 인덱스를 추가했는지 검증한다.
 * Flyway 로 V1~V6 를 실제 적용한 뒤 H2 의 INFORMATION_SCHEMA 메타데이터를 조회한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PopularAtMigrationTest {

    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private List<String> columnNamesOnPost() {
        return entityManager.createNativeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'POST'")
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<String> indexNamesOnPost() {
        return entityManager.createNativeQuery(
                        "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'POST'")
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<String> orderedColumnsOf(final String indexName) {
        return entityManager.createNativeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS "
                                + "WHERE TABLE_NAME = 'POST' AND INDEX_NAME = '" + indexName + "' "
                                + "ORDER BY ORDINAL_POSITION")
                .getResultList();
    }

    @Test
    void popular_at_컬럼이_post_테이블에_존재한다() {
        List<String> columns = columnNamesOnPost();

        assertThat(columns)
                .as("popular_at 컬럼이 post 테이블에 존재해야 한다")
                .contains("POPULAR_AT");
    }

    @Test
    void 게시판_인기글용_복합_인덱스가_존재한다() {
        List<String> indexNames = indexNamesOnPost();

        assertThat(indexNames)
                .as("idx_post_board_popular_at 복합 인덱스가 post 테이블에 존재해야 한다")
                .contains("IDX_POST_BOARD_POPULAR_AT");
    }

    @Test
    void 복합_인덱스의_컬럼_순서는_board_id_popular_at_이다() {
        List<String> columns = orderedColumnsOf("IDX_POST_BOARD_POPULAR_AT");

        assertThat(columns)
                .as("등가조건(board_id) → 정렬(popular_at) 순서여야 한다")
                .containsExactly("BOARD_ID", "POPULAR_AT");
    }
}
