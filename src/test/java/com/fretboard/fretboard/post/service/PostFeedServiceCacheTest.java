package com.fretboard.fretboard.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.config.PopularProperties;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Task 5 (인기글 캐시) 동작 테스트.
 *
 * 순수 Mockito 단위 테스트({@link PostFeedServiceTest})로는 @Cacheable 프록시가 동작하지 않으므로,
 * 캐싱을 활성화한 최소 Spring 컨텍스트에서 캐시어사이드 의미(히트/미스, TTL 만료 후 재조회)를 검증한다.
 *
 * <p>가정(GREEN 정렬 필요):
 * <ul>
 *   <li>CacheManager 빈 이름은 "postCacheManager"(getRecentPosts/게시판 캐시와 동일 규칙).</li>
 *   <li>{@code getTodayPopularPosts()} → {@code @Cacheable(cacheNames = CacheKey.POPULAR_TODAY,
 *       key = CacheKey.POPULAR_TODAY_KEY, cacheManager = "postCacheManager")}.</li>
 *   <li>{@code getPopularPostsByBoardId(boardId, pageable)} →
 *       {@code @Cacheable(cacheNames = CacheKey.POPULAR_BY_BOARD,
 *       key = "#boardId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
 *       cacheManager = "postCacheManager")} — (boardId, page, size) 별로 키가 분리된다.</li>
 *   <li>ConcurrentMapCacheManager를 사용하며 "popularToday"/"popularByBoard" 캐시를 사전 등록한다.</li>
 *   <li>Redis 없이 동작한다.</li>
 * </ul>
 *
 * <p>참고: 실제 시간 기반 TTL 만료는 Redis 없이 단위 테스트로 검증 불가하므로,
 * TTL 만료를 "엔트리 제거(clear)"로 시뮬레이션해 재조회 시 캐시 재적재(cache-aside)를 검증한다.
 * TTL 값 자체(POPULAR_TTL_SECONDS=60)는 Task 1의 RedisCacheConfigTest에서 검증된다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PostFeedService.class,
        PostFeedServiceCacheTest.CacheTestConfig.class
})
class PostFeedServiceCacheTest {

    private static final PopularProperties.Weights WEIGHTS =
            new PopularProperties.Weights(1.0, 3.0, 2.0);

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager postCacheManager() {
            return new ConcurrentMapCacheManager(CacheKey.POPULAR_TODAY, CacheKey.POPULAR_BY_BOARD);
        }

        // PopularProperties 는 값 객체(record)이므로 실제 인스턴스를 빈으로 제공한다.
        // (PostFeedServiceTest 가 생성자에 실제 인스턴스를 넘기는 방식과 동일)
        @Bean
        PopularProperties popularProperties() {
            return new PopularProperties(WEIGHTS, 10, 10, Duration.ofSeconds(60));
        }
    }

    @Autowired
    private PostFeedService postFeedService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private BoardRepository boardRepository;

    @MockBean
    private CommentCountLoader commentCountLoader;

    @MockBean
    private PopularScoreService popularScoreService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    private PostSummaryDto post(Long id) {
        return new PostSummaryDto(id, "글" + id, "작성자" + id, LocalDateTime.now(), 5L);
    }

    @Test
    @DisplayName("오늘 인기글 두 번째 조회는 DB(findByPostIds)에 접근하지 않는다")
    void 오늘인기글_두번째조회는_DB미접근() {
        // given — 랭킹 ID 는 존재하므로 본문 hydration(findByPostIds)까지 도달한다
        given(popularScoreService.getTopPostIds(anyInt()))
                .willReturn(List.of(1L, 2L, 3L));
        given(postRepository.findByPostIds(List.of(1L, 2L, 3L)))
                .willReturn(List.of(post(1L), post(2L), post(3L)));

        // when — 두 번 조회
        postFeedService.getTodayPopularPosts();
        postFeedService.getTodayPopularPosts();

        // then — 두 번째는 캐시 히트라 DB hydration 은 한 번만
        verify(postRepository, times(1)).findByPostIds(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("게시판별 인기글은 (boardId, page, size)별로 캐시 키가 분리된다")
    void 게시판별인기글_페이지별_캐시키_분리() {
        // given
        Long boardId = 1L;
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 10);

        given(postRepository.findPopularPostSummaryByBoardId(anyLong(), anyInt(), anyLong()))
                .willReturn(List.of(post(10L)));
        given(postRepository.countPopularByBoardId(anyLong())).willReturn(1L);
        given(commentCountLoader.load(anyList())).willReturn(Map.of(10L, 0L));

        // when — page0 두 번(캐시 히트), page1 한 번(다른 키 → 미스)
        postFeedService.getPopularPostsByBoardId(boardId, page0);
        postFeedService.getPopularPostsByBoardId(boardId, page0);
        postFeedService.getPopularPostsByBoardId(boardId, page1);

        // then — page0 은 두 번 호출해도 DB 는 한 번(offset 0)
        verify(postRepository, times(1))
                .findPopularPostSummaryByBoardId(eq(boardId), eq(10), eq(0L));
        // page1 은 별도 키라 DB 를 다시 조회(offset 10)
        verify(postRepository, times(1))
                .findPopularPostSummaryByBoardId(eq(boardId), eq(10), eq(10L));
    }

    @Test
    @DisplayName("인기글 TTL 만료(엔트리 제거) 후 재조회 시 DB에서 다시 채운다")
    void 인기글_TTL만료_후_재조회() {
        // given
        given(popularScoreService.getTopPostIds(anyInt()))
                .willReturn(List.of(1L, 2L, 3L));
        given(postRepository.findByPostIds(List.of(1L, 2L, 3L)))
                .willReturn(List.of(post(1L), post(2L), post(3L)));

        // when — 최초 조회로 캐시 워밍 + 재조회(캐시 히트라 DB 미접근이어야 함)
        postFeedService.getTodayPopularPosts();
        postFeedService.getTodayPopularPosts();

        // then(선행) — 캐시가 살아있는 동안에는 hydration 이 한 번뿐이어야 한다
        //  (이 단정이 있어야 캐싱 부재 시 RED 로 실패한다; 없으면 매 호출 DB 로 위양성 통과)
        verify(postRepository, times(1)).findByPostIds(List.of(1L, 2L, 3L));

        // TTL 만료 시뮬레이션: popularToday 캐시 엔트리 제거
        cacheManager.getCache(CacheKey.POPULAR_TODAY).clear();

        // 재조회 → 미스라 DB 를 다시 친다(캐시 재적재)
        postFeedService.getTodayPopularPosts();

        // then — 엔트리 제거 후 재적재로 hydration 이 총 두 번 발생
        verify(postRepository, times(2)).findByPostIds(List.of(1L, 2L, 3L));
    }
}
