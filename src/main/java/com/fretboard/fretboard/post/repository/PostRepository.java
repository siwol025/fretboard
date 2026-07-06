package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = """
                select new com.fretboard.fretboard.post.dto.PostSummaryDto(
                    p.id, p.title, m.nickname, p.createdAt, p.viewCount
                )
                from Post p
                join p.member m
                where p.board.id = :boardId
            """)
    Page<PostSummaryDto> findPostSummaryByBoardId(Long boardId, Pageable pageable);

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
                ORDER BY p.created_at DESC
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
}
