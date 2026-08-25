package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
class PostLikeCountFlushSemanticsTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Post post;

    @BeforeEach
    void setUp() {
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

        post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);

        entityManager.flush();
    }

    /*
     * 회귀 방지 가드: Post 엔티티는 @DynamicUpdate 를 유지해야 한다.
     * 재현 시나리오 — managed post 로드 -> bulk incrementLikeCount(like_count 는 DB 만 +1,
     * managed 스냅샷은 그대로) -> managed promote() 로 popular_at dirty -> flush.
     * @DynamicUpdate 가 없으면 dirty flush 가 전체 컬럼 UPDATE 를 발행하고, stale 한 like_count
     * 스냅샷을 되써서 bulk 증가분(+1)을 덮어버린다(post_like 행 수 대비 영구 -1 drift).
     * @DynamicUpdate 는 변경된 컬럼(popular_at)만 UPDATE 하므로 bulk 로 갱신된 like_count 를
     * 건드리지 않는다. 이 테스트는 Post 에서 @DynamicUpdate 를 제거하는 회귀를 잠근다.
     */
    @Test
    void 벌크_증가_후_managed_post의_promote_변경이_like_count를_덮어쓰지_않는다() {
        // given: like_count == 0, popularAt == null
        assertThat(post.getLikeCount()).isEqualTo(0L);
        assertThat(post.getPopularAt()).isNull();

        // managed 엔티티 로드
        Post managed = postRepository.findById(post.getId()).orElseThrow();

        // when: 벌크 증가 후 managed 엔티티를 dirty-checking 으로 변경
        postRepository.incrementLikeCount(managed.getId());
        managed.promote(LocalDateTime.now());

        entityManager.flush();
        entityManager.clear();

        // then
        Post reloaded = postRepository.findById(post.getId()).orElseThrow();

        assertThat(reloaded.getLikeCount())
                .as("벌크 incrementLikeCount 증가분이 dirty flush 의 전체 컬럼 UPDATE 로 덮이지 않아야 한다 "
                        + "(@DynamicUpdate 로 popular_at 만 UPDATE)")
                .isEqualTo(1L);

        assertThat(reloaded.isPopular())
                .as("managed post 의 promote() 변경이 dirty-checking 으로 영속되어야 한다.")
                .isTrue();
        assertThat(reloaded.getPopularAt())
                .as("승격 시각(popularAt)이 null 이 아니어야 한다 — Post @DynamicUpdate 제거 회귀 가드")
                .isNotNull();
    }
}
