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
 * V4 마이그레이션이 커서 조회용 커버링 복합 인덱스를 추가했는지 검증한다.
 * Flyway 로 V1~V4 를 실제 적용한 뒤 H2 의 INFORMATION_SCHEMA 메타데이터를 조회한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PaginationIndexMigrationTest {

    @Autowired
    private EntityManager entityManager;

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
    void 게시판_목록용_복합_인덱스가_post_테이블에_존재한다() {
        List<String> indexNames = indexNamesOnPost();

        assertThat(indexNames)
                .as("idx_post_board_created_id 복합 인덱스가 post 테이블에 존재해야 한다")
                .contains("IDX_POST_BOARD_CREATED_ID");
    }

    @Test
    void 내_게시글용_복합_인덱스가_post_테이블에_존재한다() {
        List<String> indexNames = indexNamesOnPost();

        assertThat(indexNames)
                .as("idx_post_member_created_id 복합 인덱스가 post 테이블에 존재해야 한다")
                .contains("IDX_POST_MEMBER_CREATED_ID");
    }

    @Test
    void 게시판_목록용_인덱스의_컬럼_순서는_board_id_created_at_id_이다() {
        List<String> columns = orderedColumnsOf("IDX_POST_BOARD_CREATED_ID");

        assertThat(columns)
                .as("등가조건(board_id) → 정렬(created_at, id) 순서여야 한다")
                .containsExactly("BOARD_ID", "CREATED_AT", "ID");
    }

    @Test
    void 내_게시글용_인덱스의_컬럼_순서는_member_id_created_at_id_이다() {
        List<String> columns = orderedColumnsOf("IDX_POST_MEMBER_CREATED_ID");

        assertThat(columns)
                .as("등가조건(member_id) → 정렬(created_at, id) 순서여야 한다")
                .containsExactly("MEMBER_ID", "CREATED_AT", "ID");
    }

    @Test
    void 복합_인덱스로_대체된_중복_단일_인덱스는_제거된다() {
        List<String> indexNames = indexNamesOnPost();

        assertThat(indexNames)
                .as("복합 인덱스가 커버하는 단일 인덱스는 V4에서 제거되어야 한다")
                .doesNotContain("IDX_POST_BOARD_ID", "IDX_POST_MEMBER_ID", "IDX_POST_CREATED_AT");
    }
}
