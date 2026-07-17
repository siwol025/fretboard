package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.MyPostSummaryDto;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.MyPostListResponse;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
import com.fretboard.fretboard.post.repository.PostLikeRepository;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private final AuthorizationHelper authorizationHelper;
    private final PostLikeRepository postLikeRepository;
    private final CommentCountLoader commentCountLoader;

    @Transactional
    public Long addPost(final PostNewRequest request, final MemberAuth memberAuth) {
        Member loginMember = authorizationHelper.getMember(memberAuth);
        Board board = getBoard(request.boardId());
        board.validateWritable(loginMember);

        String convertedContent = imageService.convertTempImageUrlsToPermanent(request.content());
        Post post = request.toPost(loginMember, board, convertedContent);

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

        post.edit(request.title(), convertedContent);
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
    public PostDetailResponse getPostDetail(final Long id, final MemberAuth memberAuth) {
        Post post = postRepository.findPostDetailById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        if (!viewCountService.hasViewCount(id)) {
            viewCountService.setInitialViewCount(id, post.getViewCount());
        }

        Long updatedViewCount = viewCountService.incrementViewCount(id);

        long likeCount = postLikeRepository.countByPostId(id);
        boolean isLiked = memberAuth != null && postLikeRepository.existsByPostIdAndMemberId(id, memberAuth.memberId());

        return PostDetailResponse.of(post, updatedViewCount, likeCount, isLiked);
    }

    public MyPostListResponse findMyPosts(final MemberAuth memberAuth, Pageable pageable) {
        Long memberId = memberAuth.memberId();
        int size = pageable.getPageSize();
        long offset = (long) pageable.getPageNumber() * size;

        List<MyPostSummaryDto> rows = postRepository.findMyPostSummaryDeferred(memberId, size, offset);
        long total = postRepository.countByMemberId(memberId);

        List<Long> postIds = rows.stream()
                .map(MyPostSummaryDto::id)
                .toList();
        Map<Long, Long> commentCountMap = commentCountLoader.load(postIds);

        List<PostSummaryResponse> content = rows.stream()
                .map(row -> PostSummaryResponse.builder()
                        .id(row.id())
                        .title(row.title())
                        .author(row.author())
                        .createdAt(row.createdAt())
                        .viewCount(row.viewCount())
                        .boardId(row.boardId())
                        .boardTitle(row.boardTitle())
                        .commentCount(commentCountMap.getOrDefault(row.id(), 0L).intValue())
                        .build())
                .toList();

        return MyPostListResponse.of(new PageImpl<>(content, pageable, total));
    }

    private Board getBoard(final Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
    }
}
