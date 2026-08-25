package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.domain.PostLike;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class LikeCountBackfillTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void 백필_UPDATE는_like_count를_post_like_개수로_설정한다() {
        // given
        Board board = Board.builder()
                .title("테스트 게시판")
                .description("테스트 게시판 설명")
                .slug("backfill-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(board);

        Member author = createMember("author");
        entityManager.persist(author);

        Post target = Post.builder()
                .title("좋아요 대상 게시글")
                .content("본문")
                .member(author)
                .board(board)
                .build();
        entityManager.persist(target);

        Post control = Post.builder()
                .title("좋아요 없는 게시글")
                .content("본문")
                .member(author)
                .board(board)
                .build();
        entityManager.persist(control);

        // target 게시글에 서로 다른 3명의 멤버가 좋아요 (uq_post_like(post_id, member_id) 때문에 멤버가 달라야 함)
        for (int i = 0; i < 3; i++) {
            Member liker = createMember("liker" + i);
            entityManager.persist(liker);
            entityManager.persist(PostLike.of(target, liker));
        }

        entityManager.flush();

        // V7 이 Flyway 로 이미 실행되었으므로, 백필 문장이 실제로 재계산하는지 증명하기 위해
        // 먼저 like_count 를 잘못된 값(999)으로 강제 설정한다.
        entityManager.createNativeQuery("UPDATE post SET like_count = 999").executeUpdate();

        // when: V7 이 사용하는 것과 동일한 백필 문장 실행
        entityManager.createNativeQuery(
                        "UPDATE post p SET like_count = (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.id)")
                .executeUpdate();
        entityManager.clear();

        // then
        long targetLikeCount = readLikeCount(target.getId());
        long controlLikeCount = readLikeCount(control.getId());

        assertThat(targetLikeCount)
                .as("백필 UPDATE 는 target 게시글의 like_count 를 해당 post_like 3개로 재계산해야 한다 (999 → 3)")
                .isEqualTo(3L);
        assertThat(controlLikeCount)
                .as("백필 UPDATE 는 좋아요가 없는 control 게시글의 like_count 를 0 으로 설정해야 하며, target 과 교차 오염되지 않아야 한다")
                .isEqualTo(0L);
    }

    private Member createMember(String suffix) {
        return Member.builder()
                .username("user_" + suffix)
                .password("password")
                .nickname("nick_" + suffix)
                .role(Role.USER)
                .build();
    }

    private long readLikeCount(Long postId) {
        Object value = entityManager.createNativeQuery("SELECT like_count FROM post WHERE id = ?")
                .setParameter(1, postId)
                .getSingleResult();
        return ((Number) value).longValue();
    }
}
