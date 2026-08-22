package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostStaticView;
import com.fretboard.fretboard.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 게시글 상세의 정적 본문을 로딩하는 전용 빈.
 *
 * <p>별도 빈으로 두는 이유: 같은 클래스 내부의 {@code @Cacheable} 자가호출(self-invocation)은
 * Spring AOP 프록시를 통하지 않아 캐싱이 동작하지 않기 때문이다. {@link PostService}가 이 빈을
 * 주입받아 호출하면 프록시를 거쳐 캐시가 적용된다.
 */
@Component
@RequiredArgsConstructor
public class PostStaticViewLoader {

    private final PostRepository postRepository;

    @Cacheable(cacheNames = CacheKey.POST_DETAIL, key = CacheKey.POST_DETAIL_KEY, cacheManager = "postCacheManager")
    public PostStaticView load(final Long postId) {
        Post post = postRepository.findPostDetailById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostStaticView.from(post);
    }
}
