package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSearchSummaryDto;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import com.fretboard.fretboard.post.dto.PostWithCommentCountDto;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.PostSearchListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostFeedService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ViewCountService viewCountService;

    public PostListResponse getPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<PostSummaryDto> posts = postRepository.findPostSummaryByBoardId(boardId, pageable);

        List<Long> postIds = posts.getContent().stream()
                .map(PostSummaryDto::id)
                .toList();

        Map<Long, Long> commentCountMap = buildCommentCountMap(postIds);

        Page<PostWithCommentCountDto> resultPage = new PageImpl<>(
                posts.getContent().stream()
                        .map(post -> new PostWithCommentCountDto(
                                post.id(),
                                post.title(),
                                post.author(),
                                post.createdAt(),
                                post.viewCount(),
                                commentCountMap.getOrDefault(post.id(), 0L)
                        )).toList(),
                pageable,
                posts.getTotalElements()
        );

        return PostListResponse.of(resultPage);
    }

    public PostSearchListResponse searchPosts(final Long boardId, final String keyword, Pageable pageable) {
        Page<PostSearchResultProjection> posts = postRepository.searchByBoardIdAndKeyword(
                boardId, keyword, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));

        List<Long> postIds = posts.getContent().stream()
                .map(PostSearchResultProjection::getId)
                .toList();

        Map<Long, Long> commentCountMap = buildCommentCountMap(postIds);

        Page<PostSearchSummaryDto> resultPage = new PageImpl<>(
                posts.getContent().stream()
                        .map(post -> new PostSearchSummaryDto(
                                post.getId(),
                                post.getTitle(),
                                post.getAuthor(),
                                post.getBoardId(),
                                post.getBoardTitle(),
                                post.getCreatedAt(),
                                post.getViewCount(),
                                commentCountMap.getOrDefault(post.getId(), 0L)
                        )).toList(),
                pageable,
                posts.getTotalElements()
        );

        return PostSearchListResponse.of(resultPage);
    }

    public List<PostSummaryDto> getMostPosts() {
        List<Long> topPosts = viewCountService.getTopPosts(5);
        List<PostSummaryDto> posts = postRepository.findByPostIds(topPosts);

        Map<Long, PostSummaryDto> postMap = posts.stream()
                .collect(Collectors.toMap(PostSummaryDto::id, Function.identity()));

        return topPosts.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Cacheable(
            cacheNames = CacheKey.RECENT_POSTS,
            key = CacheKey.RECENT_POSTS_KEY,
            cacheManager = "postCacheManager"
    )
    public List<RecentPostsPerBoardResponse> getRecentPosts() {
        return RecentPostsPerBoardResponse.of(postRepository.findRecentPostsPerBoards());
    }

    private Map<Long, Long> buildCommentCountMap(List<Long> postIds) {
        List<PostCommentCountDto> counts = commentRepository.countCommentsByPostIds(postIds);
        return counts.stream()
                .collect(Collectors.toMap(PostCommentCountDto::postId, PostCommentCountDto::commentCount));
    }
}
