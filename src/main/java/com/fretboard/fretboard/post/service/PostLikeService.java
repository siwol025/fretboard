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

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final AuthorizationHelper authorizationHelper;
    private final PopularScoreService popularScoreService;
    private final Clock clock;
    private final com.fretboard.fretboard.global.config.PopularProperties popularProperties;

    @Transactional
    public void toggleLike(Long postId, MemberAuth memberAuth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        Member member = authorizationHelper.getMember(memberAuth);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberAuth.memberId());
        if (existingLike.isEmpty()) {
            addLike(post, member, postId);
            promoteIfPopular(post, postId);
            popularScoreService.applyLike(postId);
        } else {
            removeLike(existingLike.get(), postId);
            popularScoreService.cancelLike(postId);
        }
    }

    /*
     * Like-count invariant: the post_like rows are the single source of truth; post.like_count is a
     * denormalized cache kept in lockstep with them. Every post_like insert/delete MUST go through
     * addLike/removeLike so the row mutation and its paired counter update always happen together —
     * never mutate post_like rows without the matching increment/decrement, or the two will drift.
     */
    private void addLike(Post post, Member member, Long postId) {
        postLikeRepository.save(PostLike.of(post, member));
        postRepository.incrementLikeCount(postId);
    }

    private void removeLike(PostLike existingLike, Long postId) {
        postLikeRepository.delete(existingLike);
        postRepository.decrementLikeCount(postId);
    }

    private void promoteIfPopular(Post post, Long postId) {
        if (postRepository.findLikeCountById(postId) >= popularProperties.likeThreshold() && !post.isPopular()) {
            post.promote(LocalDateTime.now(clock));
        }
    }
}
