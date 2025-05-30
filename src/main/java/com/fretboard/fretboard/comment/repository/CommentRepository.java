package com.fretboard.fretboard.comment.repository;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findCommentsByPostId(Long postId);

    @Query("""
        select new com.fretboard.fretboard.comment.dto.PostCommentCountDto(
            p.id, count(c)
        )
        from Post p
        left join p.comments c
        where p.id in :postIds
        group by p.id
    """)
    List<PostCommentCountDto> countCommentsByPostIds(List<Long> postIds);

}
