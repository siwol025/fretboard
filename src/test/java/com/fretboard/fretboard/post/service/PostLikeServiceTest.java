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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), KST);

    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthorizationHelper authorizationHelper;

    @Mock
    private PopularScoreService popularScoreService;

    private Member member;
    private MemberAuth memberAuth;
    private Post post;
    private Long postId;

    @BeforeEach
    void setUp() {
        postLikeService = new PostLikeService(
                postLikeRepository, postRepository, authorizationHelper, popularScoreService, FIXED_CLOCK);
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

    @Test
    void 좋아요_추가로_임계값_10_도달시_게시글_승격됨() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.empty());
        given(postLikeRepository.countByPostId(postId)).willReturn(10L);

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        assertThat(post.isPopular()).isTrue();
        assertThat(post.getPopularAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    void 좋아요_추가했지만_임계값_미만이면_승격_안됨() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.empty());
        given(postLikeRepository.countByPostId(postId)).willReturn(9L);

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        assertThat(post.isPopular()).isFalse();
    }

    @Test
    void 좋아요_추가시_applyLike_호출() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.empty());

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        verify(popularScoreService).applyLike(postId);
    }

    @Test
    void 좋아요_취소시_cancelLike_호출() {
        // given
        PostLike existingLike = PostLike.of(post, member);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.of(existingLike));

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        verify(popularScoreService).cancelLike(postId);
    }

    @Test
    void 좋아요_취소시_승격판정_하지_않음() {
        // given
        PostLike existingLike = PostLike.of(post, member);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postLikeRepository.findByPostIdAndMemberId(postId, 1L)).willReturn(Optional.of(existingLike));

        // when
        postLikeService.toggleLike(postId, memberAuth);

        // then
        assertThat(post.isPopular()).isFalse();
        verify(postLikeRepository, never()).countByPostId(any());
    }
}
