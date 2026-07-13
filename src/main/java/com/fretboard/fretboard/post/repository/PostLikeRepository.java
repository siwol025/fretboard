package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.domain.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);

    long countByPostId(Long postId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
