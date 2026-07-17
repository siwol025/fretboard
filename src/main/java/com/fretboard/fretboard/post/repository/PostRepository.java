package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.MyPostSummaryDto;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    default List<PostSummaryDto> findPostSummaryByBoardIdDeferred(Long boardId, int size, long offset) {
        return findPostSummaryByBoardIdDeferredRaw(boardId, size, offset).stream()
                .map(PostSummaryRowMapper::map)
                .toList();
    }

    /*
     * Deferred Join: 내부 서브쿼리(ip)에서 board_id 인덱스로 id 페이지만 먼저 추린 뒤
     * 바깥에서 본문 컬럼을 조인해 대량 오프셋의 커버링 비용을 줄인다.
     * 파생 테이블 서브쿼리에 LIMIT/OFFSET 을 둔다 — MySQL 은 IN(...LIMIT) 형태를 지원하지 않으므로
     * (ERROR 1235) 반드시 FROM 절 파생 테이블 JOIN 으로 작성해야 한다. H2 는 양쪽 다 허용.
     * ORDER BY 는 서브쿼리(페이징 대상 선택)와 외부 쿼리(최종 정렬) 양쪽 모두 필수다 —
     * 파생 테이블 JOIN 은 순서를 보장하지 않으므로 외부 ORDER BY 를 제거하면 정렬이 깨진다.
     * created_at 동률 시 id DESC 타이브레이커로 페이지 경계의 중복/누락을 방지한다.
     */
    @Query(
            value = """
                SELECT p.id, p.title, m.nickname, p.created_at, p.view_count
                FROM post p
                JOIN (
                    SELECT ip.id
                    FROM post ip
                    WHERE ip.board_id = :boardId
                    ORDER BY ip.created_at DESC, ip.id DESC
                    LIMIT :size OFFSET :offset
                ) sub ON sub.id = p.id
                JOIN member m ON m.id = p.member_id
                ORDER BY p.created_at DESC, p.id DESC
                """,
            nativeQuery = true
    )
    List<Object[]> findPostSummaryByBoardIdDeferredRaw(
            @Param("boardId") Long boardId,
            @Param("size") int size,
            @Param("offset") long offset
    );

    long countByBoardId(Long boardId);

    default List<MyPostSummaryDto> findMyPostSummaryDeferred(Long memberId, int size, long offset) {
        return findMyPostSummaryDeferredRaw(memberId, size, offset).stream()
                .map(MyPostSummaryRowMapper::map)
                .toList();
    }

    /*
     * findPostSummaryByBoardIdDeferredRaw 와 동일한 Deferred Join 전략
     * (파생 테이블 LIMIT/OFFSET, 이중 ORDER BY 필수, id DESC 타이브레이커) — 위 주석 참고.
     * 차이점: 서브쿼리 필터가 member_id 이고, 바깥에서 board 를 추가 조인해 b.id·b.title 을 함께 반환한다.
     */
    @Query(
            value = """
                SELECT p.id, p.title, m.nickname, p.created_at, p.view_count, b.id, b.title
                FROM post p
                JOIN (
                    SELECT ip.id
                    FROM post ip
                    WHERE ip.member_id = :memberId
                    ORDER BY ip.created_at DESC, ip.id DESC
                    LIMIT :size OFFSET :offset
                ) sub ON sub.id = p.id
                JOIN member m ON m.id = p.member_id
                JOIN board b ON b.id = p.board_id
                ORDER BY p.created_at DESC, p.id DESC
                """,
            nativeQuery = true
    )
    List<Object[]> findMyPostSummaryDeferredRaw(
            @Param("memberId") Long memberId,
            @Param("size") int size,
            @Param("offset") long offset
    );

    long countByMemberId(Long memberId);

    @Query(
            value = """
                SELECT p.*
                FROM (
                    SELECT p.*, ROW_NUMBER() OVER (PARTITION BY board_id ORDER BY p.created_at DESC) AS rn
                    FROM post p
                ) p
                WHERE p.rn <= 5
                ORDER BY p.board_id, p.rn
            """,
            nativeQuery = true
    )
    List<Post> findRecentPostsPerBoards();

    @Query(
            value = """
                SELECT p.id, p.title, m.nickname AS author,
                       b.id AS boardId, b.title AS boardTitle,
                       p.created_at AS createdAt, p.view_count AS viewCount
                FROM post p
                JOIN member m ON m.id = p.member_id
                JOIN board b ON b.id = p.board_id
                WHERE p.board_id = :boardId
                  AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
                ORDER BY MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE) DESC
                """,
            countQuery = """
                SELECT COUNT(*) FROM post p
                WHERE p.board_id = :boardId
                  AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
                """,
            nativeQuery = true
    )
    Page<PostSearchResultProjection> searchByBoardIdAndKeyword(@Param("boardId") Long boardId, @Param("keyword") String keyword, Pageable pageable);

    Page<Post> findByMemberId(Long memberId, Pageable pageable);

    @Query(value = """
                select new com.fretboard.fretboard.post.dto.PostSummaryDto(
                    p.id, p.title, m.nickname, p.createdAt, p.viewCount
                )
                from Post p
                join p.member m
                where p.id in :postIds
            """)
    List<PostSummaryDto> findByPostIds(List<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = :viewCount WHERE p.id = :postId")
    void updateViewCount(@Param("postId") Long postId, @Param("viewCount") Long viewCount);

    @Query("""
                SELECT p FROM Post p
                JOIN FETCH p.member
                JOIN FETCH p.board
                LEFT JOIN FETCH p.comments c
                LEFT JOIN FETCH c.member
                WHERE p.id = :id
            """)
    Optional<Post> findPostDetailById(@Param("id") Long id);
}
