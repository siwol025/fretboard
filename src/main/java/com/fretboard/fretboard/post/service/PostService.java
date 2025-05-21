package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.repository.MemberRepository;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final BoardRepository boardRepository;
    private final ViewCountService viewCountService;

    @Transactional
    public Long addPost(final PostNewRequest request, final MemberAuth memberAuth) {
        Member loginMember = getMember(memberAuth);
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

        validateIsAuthor(post.getMember(), getMember(memberAuth));

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

        validateIsAuthor(post.getMember(), getMember(memberAuth));
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

        Long updatedViewCount = viewCountService.increaseViewCount(id);

        return PostDetailResponse.of(post, updatedViewCount);
    }

    public PostListResponse findMyPosts(final MemberAuth memberAuth, Pageable pageable) {
        Page<Post> posts = postRepository.findByMemberId(memberAuth.memberId(), pageable);
        return PostListResponse.of(posts);
    }

    public PostListResponse findPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<Post> posts = postRepository.findByBoardId(boardId,pageable);
        return PostListResponse.of(posts);
    }

    public PostListResponse searchPosts(final Long boardId, final String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable);
        return PostListResponse.of(posts);
    }

    private Member getMember(final MemberAuth memberAuth) {
        return memberRepository.findById(memberAuth.memberId())
                .orElseThrow(() -> new FretBoardException(ExceptionType.MEMBER_NOT_FOUND));
    }

    private Board getBoard(final Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
    }

    private void validateIsAuthor(Member author, Member loginMember) {
        if (!author.equals(loginMember)) {
            throw new FretBoardException(ExceptionType.NOT_AUTHOR);
        }
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
            if (!postRepository.existsById(postId)) {
                continue;
            }
            postRepository.updateViewCount(postId, viewCount);
        }

        viewCountService.clearAllViewCounts();
    }
}
