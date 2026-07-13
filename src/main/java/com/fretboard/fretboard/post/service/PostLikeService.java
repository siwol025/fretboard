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

    @Transactional
    public void toggleLike(Long postId, MemberAuth memberAuth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        Member member = authorizationHelper.getMember(memberAuth);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberAuth.memberId());
        if (existingLike.isEmpty()) {
            postLikeRepository.save(PostLike.of(post, member));
        } else {
            postLikeRepository.delete(existingLike.get());
        }
    }
}
