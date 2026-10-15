package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.post.domain.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);

    // 직접 post_like 행 수를 세는 용도(쿼리/테스트)로 유지한다. 승격 판정은 이제 비정규화 카운터(findLikeCountById)를 사용한다.
    long countByPostId(Long postId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
