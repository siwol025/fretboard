package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.domain.Post;
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
            """,
            countQuery = """
                select count(p)
                from Post p
                where p.board.id = :boardId
            """)
    Page<PostSummaryDto> findByBoardIdV4(Long boardId, Pageable pageable);

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
                SELECT p
                FROM Post p
                WHERE p.board.id = :boardId
                AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
            """
    )
    Page<Post> searchByBoardIdAndKeyword(Long boardId, String keyword, Pageable pageable);

    Page<Post> findByMemberId(Long memberId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = :viewCount WHERE p.id = :postId")
    void updateViewCount(@Param("postId") Long postId, @Param("viewCount") Long viewCount);
}
