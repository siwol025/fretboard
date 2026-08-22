package com.fretboard.fretboard.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.response.CommentDetailResponse;
import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.comment.service.CommentService;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.repository.PostLikeRepository;
import com.fretboard.fretboard.post.repository.PostRepository;
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
 * Task 4 (게시글 상세 정적 본문 캐시) 동작 테스트.
 *
 * <p>순수 Mockito 단위 테스트({@link PostServiceTest})로는 @Cacheable/@CacheEvict 프록시가 동작하지 않으므로,
 * 캐싱을 활성화한 최소 Spring 컨텍스트에서 캐시어사이드 의미(히트/미스, 무효화, 동적 데이터 분리)를 검증한다.
 * {@link BoardServiceCacheTest} 패턴을 따른다.
 *
 * <p>가정(GREEN 정렬 필요):
 * <ul>
 *   <li>테스트 CacheManager 빈 이름은 "postCacheManager"이며 "postDetail" 캐시를 사전 등록한다.</li>
 *   <li>{@link PostStaticViewLoader}는 실제 빈으로 컨텍스트에 올리고, {@code load}에
 *       {@code @Cacheable(postDetail, key=#postId, cacheManager="postCacheManager")}가 붙어야 캐시 히트가 성립한다(GREEN).</li>
 *   <li>{@link PostService#getPostDetail}은 정적 본문을 {@code PostStaticViewLoader.load(id)}로,
 *       댓글을 {@code CommentService.findComments(id)}로 소싱하도록 GREEN에서 합성이 재구성된다.</li>
 *   <li>조회수({@link ViewCountService})·좋아요/isLiked는 매 조회 동적 처리(캐시 안 함).</li>
 *   <li>Redis 없이 ConcurrentMapCacheManager로 동작한다.</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PostService.class,
        PostStaticViewLoader.class,
        PostServiceCacheTest.CacheTestConfig.class
})
class PostServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager postCacheManager() {
            // recentPosts: updatePost/deletePost의 기존 @CacheEvict가 참조(사전 등록해 인프라 오류 회피).
            // postDetail: Task 4 정적 본문 캐시(GREEN에서 로더 @Cacheable + update/delete evict 대상).
            return new ConcurrentMapCacheManager(CacheKey.RECENT_POSTS, CacheKey.POST_DETAIL);
        }
    }

    private static final Long POST_ID = 1L;
    private final MemberAuth memberAuth = new MemberAuth(10L);

    @Autowired
    private PostService postService;

    // PostStaticViewLoader의 실 의존 + PostService의 findPostDetailById 소싱 경로
    @MockBean
    private PostRepository postRepository;

    // PostService 생성자 협력자들(현 시점 기준). GREEN에서 일부가 미사용이 되어도 컨텍스트 등록은 무해.
    @MockBean
    private ImageService imageService;

    @MockBean
    private BoardRepository boardRepository;

    @MockBean
    private ViewCountService viewCountService;

    @MockBean
    private AuthorizationHelper authorizationHelper;

    @MockBean
    private PostLikeRepository postLikeRepository;

    @MockBean
    private CommentCountLoader commentCountLoader;

    @MockBean
    private PopularScoreService popularScoreService;

    // GREEN에서 상세가 댓글을 소싱할 협력자.
    @MockBean
    private CommentService commentService;

    @Autowired
    private CacheManager cacheManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", 10L);

        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("상세 정적 본문 두 번째 조회는 DB에 접근하지 않는다")
    void 상세정적본문_두번째조회는_DB미접근() {
        givenPostDetail();
        givenDynamicData();

        postService.getPostDetail(POST_ID, memberAuth);
        postService.getPostDetail(POST_ID, memberAuth);

        // 두 번째 조회는 postDetail 캐시 히트 → findPostDetailById는 한 번만 호출된다.
        verify(postRepository, times(1)).findPostDetailById(POST_ID);
    }

    @Test
    @DisplayName("상세 조회수는 매번 증가한다(동적, 미캐싱)")
    void 상세조회수는_매번_증가() {
        givenPostDetail();
        givenDynamicData();

        postService.getPostDetail(POST_ID, memberAuth);
        postService.getPostDetail(POST_ID, memberAuth);

        // 정적 본문은 캐시되어도 조회수는 매 조회 증가해야 한다.
        verify(viewCountService, times(2)).incrementViewCount(POST_ID);
    }

    @Test
    @DisplayName("게시글 수정 후 상세 캐시가 무효화된다")
    void 게시글수정_후_상세캐시_무효화() {
        givenPostDetail();
        givenDynamicData();
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post()));
        given(imageService.convertTempImageUrlsToPermanent(anyString()))
                .willReturn("수정된 본문");

        // 워밍: postDetail 캐시에 정적 본문 적재
        postService.getPostDetail(POST_ID, memberAuth);
        assertThat(cacheManager.getCache(CacheKey.POST_DETAIL).get(POST_ID)).isNotNull();

        postService.updatePost(POST_ID, new PostEditRequest("수정된 제목", "수정된 본문"), memberAuth);

        // 무효화 검증: 캐시 엔트리가 제거되어야 한다.
        assertThat(cacheManager.getCache(CacheKey.POST_DETAIL).get(POST_ID)).isNull();

        // 다음 조회는 DB를 다시 친다 → findPostDetailById 총 2회.
        postService.getPostDetail(POST_ID, memberAuth);
        verify(postRepository, times(2)).findPostDetailById(POST_ID);
    }

    @Test
    @DisplayName("게시글 삭제 후 상세 캐시가 무효화된다")
    void 게시글삭제_후_상세캐시_무효화() {
        givenPostDetail();
        givenDynamicData();
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post()));

        // 워밍
        postService.getPostDetail(POST_ID, memberAuth);
        assertThat(cacheManager.getCache(CacheKey.POST_DETAIL).get(POST_ID)).isNotNull();

        postService.deletePost(POST_ID, memberAuth);

        // postDetail(#id) 엔트리가 제거되어야 한다.
        assertThat(cacheManager.getCache(CacheKey.POST_DETAIL).get(POST_ID)).isNull();
    }

    @Test
    @DisplayName("댓글 변경 후 상세 조회 시 최신 댓글이 반영된다(댓글은 comments 캐시에서 라이브 소싱)")
    void 댓글변경_후_상세조회시_최신댓글반영() {
        givenPostDetail();
        givenDynamicData();

        CommentResponse first = new CommentResponse(List.of(
                CommentDetailResponse.builder().id(1L).content("첫 댓글").authorId(10L).author("testnick").build()));
        CommentResponse second = new CommentResponse(List.of(
                CommentDetailResponse.builder().id(1L).content("첫 댓글").authorId(10L).author("testnick").build(),
                CommentDetailResponse.builder().id(2L).content("둘째 댓글").authorId(10L).author("testnick").build()));
        given(commentService.findComments(POST_ID)).willReturn(first, second);

        PostDetailResponse r1 = postService.getPostDetail(POST_ID, memberAuth);
        PostDetailResponse r2 = postService.getPostDetail(POST_ID, memberAuth);

        // 정적 본문은 캐시되지만(1회 DB), 댓글은 매 조회 commentService에서 소싱되어 최신 상태를 반영한다.
        verify(postRepository, times(1)).findPostDetailById(POST_ID);
        verify(commentService, times(2)).findComments(POST_ID);

        assertThat(r1.commentResponse().contents()).hasSize(1);
        assertThat(r2.commentResponse().contents()).hasSize(2);
        assertThat(r2.commentResponse().contents())
                .extracting(CommentDetailResponse::content)
                .containsExactly("첫 댓글", "둘째 댓글");
    }

    private void givenPostDetail() {
        given(postRepository.findPostDetailById(POST_ID)).willReturn(Optional.of(post()));
    }

    private void givenDynamicData() {
        given(viewCountService.hasViewCount(POST_ID)).willReturn(true);
        given(viewCountService.incrementViewCount(POST_ID)).willReturn(1L);
        given(postLikeRepository.countByPostId(POST_ID)).willReturn(0L);
        given(postLikeRepository.existsByPostIdAndMemberId(POST_ID, 10L)).willReturn(false);
    }

    private Post post() {
        Board board = Board.builder()
                .title("자유게시판")
                .description("자유롭게 작성하는 게시판")
                .slug("free")
                .boardType(BoardType.WRITABLE)
                .build();
        ReflectionTestUtils.setField(board, "id", 20L);

        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .member(member)
                .board(board)
                .build();
        ReflectionTestUtils.setField(post, "id", POST_ID);
        List<Comment> comments = List.of(Comment.parent("본문에 딸린 댓글", member, post));
        ReflectionTestUtils.setField(post, "comments", comments);
        return post;
    }
}
