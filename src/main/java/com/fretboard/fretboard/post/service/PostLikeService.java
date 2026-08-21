package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import java.util.Optional;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.domain.PostLike;
import com.fretboard.fretboard.post.repository.PostLikeRepository;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private static final long POPULAR_LIKE_THRESHOLD = 10;

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final AuthorizationHelper authorizationHelper;
    private final PopularScoreService popularScoreService;
    private final Clock clock;

    @Transactional
    public void toggleLike(Long postId, MemberAuth memberAuth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        Member member = authorizationHelper.getMember(memberAuth);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberAuth.memberId());
        if (existingLike.isEmpty()) {
            postLikeRepository.save(PostLike.of(post, member));
            promoteIfPopular(post, postId);
            popularScoreService.applyLike(postId);
        } else {
            postLikeRepository.delete(existingLike.get());
            popularScoreService.cancelLike(postId);
        }
    }

    private void promoteIfPopular(Post post, Long postId) {
        if (postLikeRepository.countByPostId(postId) >= POPULAR_LIKE_THRESHOLD && !post.isPopular()) {
            post.promote(LocalDateTime.now(clock));
        }
    }
}
