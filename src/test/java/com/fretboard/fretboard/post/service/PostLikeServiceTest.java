package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.domain.PostLike;
import com.fretboard.fretboard.post.repository.PostLikeRepository;
import com.fretboard.fretboard.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthorizationHelper authorizationHelper;

    private Member member;
    private MemberAuth memberAuth;
    private Post post;
    private Long postId;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        memberAuth = new MemberAuth(1L);
        post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(null)
                .build();
        postId = 10L;
    }

    @Test
    void toggleLike_첫_좋아요_저장됨() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.empty());

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    void toggleLike_이미_좋아요_있으면_삭제됨() {
        // given
        PostLike existingLike = PostLike.of(post, member);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.of(existingLike));

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        verify(postLikeRepository).delete(existingLike);
        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    void toggleLike_게시글없으면_POST_NOT_FOUND_예외() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.toggleLike(postId, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.POST_NOT_FOUND);
                });
    }
}
