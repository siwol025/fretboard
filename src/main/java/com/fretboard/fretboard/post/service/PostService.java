package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.helper.AuthorizationHelper;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSearchSummaryDto;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.MyPostListResponse;
import com.fretboard.fretboard.post.dto.response.PostSearchListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.PostWithCommentCountDto;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final ImageService imageService;
    private final BoardRepository boardRepository;
    private final ViewCountService viewCountService;
    private final CommentRepository commentRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public Long addPost(final PostNewRequest request, final MemberAuth memberAuth) {
        Member loginMember = authorizationHelper.getMember(memberAuth);
        Board board = getBoard(request.boardId());
        board.validateWritable(loginMember);
        Post post = request.toPost(loginMember, board);

        String convertedContent = imageService.convertTempImageUrlsToPermanent(post.getContent());
        post.setContent(convertedContent);

        Post savedPost = postRepository.save(post);
        return savedPost.getId();
    }

    @CacheEvict(value = CacheKey.RECENT_POSTS, key = CacheKey.RECENT_POSTS_KEY)
    @Transactional
    public void updatePost(final Long id, final PostEditRequest request, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        authorizationHelper.validateIsAuthor(post.getMember(), authorizationHelper.getMember(memberAuth));

        imageService.cleanUpRemovedImages(post.getContent(), request.content());
        String convertedContent = imageService.convertTempImageUrlsToPermanent(request.content());

        post.setTitle(request.title());
        post.setContent(convertedContent);
    }

    @CacheEvict(value = CacheKey.RECENT_POSTS, key = CacheKey.RECENT_POSTS_KEY)
    @Transactional
    public void deletePost(final Long id, final MemberAuth memberAuth) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        authorizationHelper.validateIsAuthor(post.getMember(), authorizationHelper.getMember(memberAuth));
        imageService.deleteImage(post.getContent());

        postRepository.delete(post);
        viewCountService.deleteViewCount(id);
    }

    @Transactional
    public PostDetailResponse getPostDetail(final Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        if (!viewCountService.hasViewCount(id)) {
            viewCountService.setInitialViewCount(id, post.getViewCount());
        }

        Long updatedViewCount = viewCountService.incrementViewCount(id);

        return PostDetailResponse.of(post, updatedViewCount);
    }

    public MyPostListResponse findMyPosts(final MemberAuth memberAuth, Pageable pageable) {
        Page<Post> posts = postRepository.findByMemberId(memberAuth.memberId(), pageable);
        return MyPostListResponse.of(posts);
    }

    public PostListResponse getPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<PostSummaryDto> posts = postRepository.findPostSummaryByBoardId(boardId, pageable);

        List<Long> postIds = posts.getContent().stream()
                .map(PostSummaryDto::id)
                .toList();

        List<PostCommentCountDto> counts = commentRepository.countCommentsByPostIds(postIds);
        Map<Long, Long> commentCountMap = counts.stream()
                .collect(Collectors.toMap(PostCommentCountDto::postId, PostCommentCountDto::commentCount));

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
        Page<PostSearchResultProjection> posts = postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable);

        List<Long> postIds = posts.getContent().stream()
                .map(PostSearchResultProjection::getId)
                .toList();

        List<PostCommentCountDto> counts = commentRepository.countCommentsByPostIds(postIds);
        Map<Long, Long> commentCountMap = counts.stream()
                .collect(Collectors.toMap(PostCommentCountDto::postId, PostCommentCountDto::commentCount));

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

    private Board getBoard(final Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
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
        List<Post> recentPosts = postRepository.findRecentPostsPerBoards();
        return RecentPostsPerBoardResponse.of(recentPosts);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncViewCountsToDatabase() {
        Map<Object, Object> counts = viewCountService.getAllViewCounts();

        for (Map.Entry<Object, Object> entry : counts.entrySet()) {
            Long postId = Long.parseLong(entry.getKey().toString());
            Long viewCount = Long.parseLong(entry.getValue().toString());
            postRepository.updateViewCount(postId, viewCount);
        }

        viewCountService.clearAllViewCounts();
    }
}
