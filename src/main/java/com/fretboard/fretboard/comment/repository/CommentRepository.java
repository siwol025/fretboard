package com.fretboard.fretboard.comment.repository;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findCommentsByPostIdWithMember(@Param("postId") Long postId);

    @Query("""
        select new com.fretboard.fretboard.comment.dto.PostCommentCountDto(
            p.id, count(c)
        )
        from Post p
        left join p.comments c
        where p.id in :postIds
          and (c is null or c.deletedAt is null)
        group by p.id
    """)
    List<PostCommentCountDto> countCommentsByPostIds(List<Long> postIds);

}
