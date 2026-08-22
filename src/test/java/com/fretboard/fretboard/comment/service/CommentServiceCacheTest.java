package com.fretboard.fretboard.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
import com.fretboard.fretboard.post.service.PopularScoreService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Task 3 (댓글 캐시) 동작 테스트.
 *
 * 순수 Mockito 단위 테스트({@link CommentServiceTest})로는 @Cacheable/@CacheEvict 프록시가 동작하지 않으므로,
 * 캐싱을 활성화한 최소 Spring 컨텍스트에서 캐시어사이드 의미(히트/미스, 무효화)를 검증한다.
 * ({@link com.fretboard.fretboard.board.service.BoardServiceCacheTest} 패턴 재사용)
 *
 * <p>가정(GREEN 정렬 필요):
 * <ul>
 *   <li>테스트 CacheManager 빈 이름은 프로덕션 규칙과 맞추기 위해 "postCacheManager"로 등록한다
 *       (기존 캐싱 경로가 cacheManager = "postCacheManager"를 명시하므로,
 *        CommentService @Cacheable/@CacheEvict도 동일 cacheManager를 사용할 것으로 가정).</li>
 *   <li>ConcurrentMapCacheManager를 사용하며 "comments" 캐시를 사전 등록한다(동적 생성에 의존하지 않음).</li>
 *   <li>findComments(postId)는 @Cacheable(comments, key=#postId)로 postId를 캐시 키로 사용한다.</li>
 *   <li>addComment(postId)는 @CacheEvict(comments, #postId)로 선언적 무효화한다.</li>
 *   <li>editComment(id)/deleteComment(id)는 comment.getPost().getId()로 해석한 postId를
 *       CacheManager(또는 소형 evictor)로 프로그래밍 방식 evict한다.</li>
 *   <li>Redis 없이 동작한다.</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CommentService.class,
        CommentServiceCacheTest.CacheTestConfig.class
})
class CommentServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager postCacheManager() {
            return new ConcurrentMapCacheManager(CacheKey.COMMENTS);
        }
    }

    @Autowired
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private AuthorizationHelper authorizationHelper;

    @MockBean
    private PopularScoreService popularScoreService;

    @Autowired
    private CacheManager cacheManager;

    private final MemberAuth memberAuth = new MemberAuth(1L);

    private Member member;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());

        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
    }

    private Post postWithId(Long postId) {
        Post post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(null)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    @Test
    @DisplayName("댓글 목록 두 번째 조회는 DB에 접근하지 않는다")
    void 댓글목록_두번째조회는_DB미접근() {
        Long postId = 10L;
        Post post = postWithId(postId);
        given(commentRepository.findCommentsByPostIdWithMember(postId))
                .willReturn(List.of(Comment.parent("댓글 내용", member, post)));

        commentService.findComments(postId);
        commentService.findComments(postId);

        verify(commentRepository, times(1)).findCommentsByPostIdWithMember(postId);
    }

    @Test
    @DisplayName("댓글 작성 후 댓글 캐시가 무효화된다")
    void 댓글작성_후_댓글캐시_무효화() {
        Long postId = 10L;
        Post post = postWithId(postId);
        given(commentRepository.findCommentsByPostIdWithMember(postId))
                .willReturn(List.of(Comment.parent("댓글 내용", member, post)));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(commentRepository.save(any(Comment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 캐시 워밍: comments(postId) 엔트리가 채워진다.
        commentService.findComments(postId);
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNotNull();

        commentService.addComment(postId, new CommentRequest("새 댓글"), memberAuth);

        // 무효화: comments(postId) 엔트리가 제거되었다.
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNull();

        // 재질의: 다음 findComments(postId)는 DB를 다시 조회한다.
        commentService.findComments(postId);
        verify(commentRepository, times(2)).findCommentsByPostIdWithMember(postId);
    }

    @Test
    @DisplayName("댓글 수정 후 해석된 postId로 캐시가 무효화된다")
    void 댓글수정_후_해석된postId로_무효화() {
        Long postId = 10L;
        Long commentId = 20L;
        Post post = postWithId(postId);
        Comment comment = Comment.parent("원래 댓글 내용", member, post);

        given(commentRepository.findCommentsByPostIdWithMember(postId))
                .willReturn(List.of(comment));
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);

        // 캐시 워밍: comments(postId) 엔트리가 채워진다.
        commentService.findComments(postId);
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNotNull();

        // editComment는 파라미터에 postId가 없으므로 comment.getPost().getId()로 해석해 evict한다.
        commentService.editComment(commentId, new CommentRequest("수정된 댓글 내용"), memberAuth);

        // 무효화: 해석된 postId의 comments 엔트리가 제거되었다.
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNull();
    }

    @Test
    @DisplayName("댓글 삭제 후 해석된 postId로 캐시가 무효화된다")
    void 댓글삭제_후_해석된postId로_무효화() {
        Long postId = 10L;
        Long commentId = 30L;
        Post post = postWithId(postId);
        Comment comment = Comment.parent("댓글 내용", member, post);

        given(commentRepository.findCommentsByPostIdWithMember(postId))
                .willReturn(List.of(comment));
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);

        // 캐시 워밍: comments(postId) 엔트리가 채워진다.
        commentService.findComments(postId);
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNotNull();

        // deleteComment도 파라미터에 postId가 없으므로 comment.getPost().getId()로 해석해 evict한다.
        commentService.deleteComment(commentId, memberAuth);

        // 무효화: 해석된 postId의 comments 엔트리가 제거되었다.
        assertThat(cacheManager.getCache(CacheKey.COMMENTS).get(postId)).isNull();
    }
}
