package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
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
 * V7 마이그레이션이 post 테이블에 like_count 컬럼(BIGINT, NOT NULL, DEFAULT 0)을 추가하고
 * 기존 행을 post_like 개수로 백필하는지 검증한다.
 * Flyway 로 V1~V7 을 실제 적용한 뒤 H2 의 INFORMATION_SCHEMA 메타데이터와 실제 값을 조회한다.
 *
 * 주의: Flyway 는 테스트 본문 실행 전에 수행되므로, 백필 대상(선존재 행)을 테스트 내에서
 * 삽입해 검증할 수 없다. 따라서 스키마 존재와 DEFAULT 0 시맨틱(좋아요 없는 신규 게시글의
 * like_count == 0)만 검증한다. 동시성/카운팅 동작은 이후 사이클에서 다룬다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class LikeCountMigrationTest {

    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private List<String> columnNamesOnPost() {
        return entityManager.createNativeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'POST'")
                .getResultList();
    }

    @Test
    void like_count_컬럼이_post_테이블에_존재한다() {
        List<String> columns = columnNamesOnPost();

        assertThat(columns)
                .as("like_count 컬럼이 post 테이블에 존재해야 한다")
                .contains("LIKE_COUNT");
    }

    @Test
    void 기존_게시글의_like_count가_post_like_개수로_백필된다() {
        // given: 좋아요가 없는 신규 게시글을 저장한다
        Member member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        entityManager.persist(member);

        Board board = Board.builder()
                .title("테스트 게시판")
                .description("테스트 게시판 설명")
                .slug("test-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(board);

        Post post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);

        entityManager.flush();
        entityManager.clear();

        // when: 저장된 게시글의 like_count 를 네이티브 쿼리로 다시 읽는다
        Number likeCount = (Number) entityManager.createNativeQuery(
                        "SELECT like_count FROM post WHERE id = ?")
                .setParameter(1, post.getId())
                .getSingleResult();

        // then: 좋아요가 없으므로 DEFAULT 0 이어야 한다
        assertThat(likeCount.longValue())
                .as("좋아요가 없는 게시글의 like_count 는 0 이어야 한다")
                .isEqualTo(0L);
    }
}
