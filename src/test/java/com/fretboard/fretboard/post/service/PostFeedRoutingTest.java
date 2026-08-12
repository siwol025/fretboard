package com.fretboard.fretboard.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fretboard.fretboard.global.datasource.ReadWorkload;
import com.fretboard.fretboard.global.datasource.WorkloadContextHolder;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@code PostFeedService} 의 워크로드 분류(@Analytics) 검증.
 *
 * <p>라우팅 자체는 {@code WorkloadRoutingIntegrationTest} 에서 E2E 로 검증됐다. 여기서는
 * 실제 Spring AOP 프록시된 서비스 메서드가 실행되는 동안 {@link WorkloadContextHolder#getWorkload()}
 * 값이 무엇으로 세팅되는지를 협력 리포지토리 스텁 시점에 캡처해, {@code @Analytics} 애노테이션과
 * {@code WorkloadRoutingAspect} 발화를 검증한다.
 *
 * <p>협력자는 모두 {@link MockBean} 으로 대체하여 H2/컨테이너 없이 빠르게 돈다.
 * 각 서비스 메서드가 호출하는 주 리포지토리 메서드를 {@code thenAnswer} 로 스텁해 그 순간의
 * 워크로드를 캡처한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostFeedRoutingTest {

    @Autowired
    private PostFeedService postFeedService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private ViewCountService viewCountService;

    @MockBean
    private CommentCountLoader commentCountLoader;

    // @Cacheable(cacheManager = "postCacheManager") 가 Redis 로 나가지 않도록 no-op 캐시로 대체.
    // getCache 는 항상 미스(get→null)를 반환하는 Cache 를 주어 서비스 본문이 항상 실행되게 한다.
    @MockBean(name = "postCacheManager")
    private CacheManager postCacheManager;

    private ReadWorkload capturedWorkload;

    @AfterEach
    void tearDown() {
        WorkloadContextHolder.clear();
    }

    private void stubAlwaysMissCache() {
        Cache cache = Mockito.mock(Cache.class);
        when(cache.get(any())).thenReturn(null);
        when(postCacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Test
    void searchPosts_실행중_ANALYTICS_워크로드() {
        Page<PostSearchResultProjection> emptyPage = new PageImpl<>(List.of());
        when(postRepository.searchByBoardIdAndKeyword(anyLong(), anyString(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    capturedWorkload = WorkloadContextHolder.getWorkload();
                    return emptyPage;
                });
        when(commentCountLoader.load(any())).thenReturn(Map.of());

        postFeedService.searchPosts(1L, "keyword", PageRequest.of(0, 20));

        assertThat(capturedWorkload)
                .as("searchPosts 실행 중에는 ANALYTICS 워크로드여야 한다")
                .isEqualTo(ReadWorkload.ANALYTICS);
    }

    @Test
    void getMostPosts_실행중_ANALYTICS_워크로드() {
        when(viewCountService.getTopPosts(anyInt())).thenReturn(List.of());
        when(postRepository.findByPostIds(any()))
                .thenAnswer(invocation -> {
                    capturedWorkload = WorkloadContextHolder.getWorkload();
                    return List.of();
                });

        postFeedService.getMostPosts();

        assertThat(capturedWorkload)
                .as("getMostPosts 실행 중에는 ANALYTICS 워크로드여야 한다")
                .isEqualTo(ReadWorkload.ANALYTICS);
    }

    @Test
    void getRecentPosts_실행중_ANALYTICS_워크로드() {
        stubAlwaysMissCache();
        when(postRepository.findRecentPostsPerBoards())
                .thenAnswer(invocation -> {
                    capturedWorkload = WorkloadContextHolder.getWorkload();
                    return List.of();
                });

        postFeedService.getRecentPosts();

        assertThat(capturedWorkload)
                .as("getRecentPosts 실행 중에는 ANALYTICS 워크로드여야 한다")
                .isEqualTo(ReadWorkload.ANALYTICS);
    }

    @Test
    void getPostsByBoardId_실행중_SERVING_워크로드() {
        // 딥페이지(큰 offset)로 호출해도 SERVING 이 유지돼야 한다(SERVING 유지 가드).
        when(postRepository.findPostSummaryByBoardIdDeferred(anyLong(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    capturedWorkload = WorkloadContextHolder.getWorkload();
                    return List.of();
                });
        when(postRepository.countByBoardId(anyLong())).thenReturn(0L);
        when(commentCountLoader.load(any())).thenReturn(Map.of());

        postFeedService.getPostsByBoardId(1L, PageRequest.of(10_000, 20));

        assertThat(capturedWorkload)
                .as("getPostsByBoardId 는 딥페이지여도 SERVING 워크로드를 유지해야 한다")
                .isEqualTo(ReadWorkload.SERVING);
    }
}
