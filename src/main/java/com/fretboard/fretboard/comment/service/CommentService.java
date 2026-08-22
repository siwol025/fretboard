package com.fretboard.fretboard.comment.service;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.global.utils.HtmlSanitizer;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
import com.fretboard.fretboard.post.service.PopularScoreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthorizationHelper authorizationHelper;
    private final PopularScoreService popularScoreService;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(cacheNames = CacheKey.COMMENTS, key = CacheKey.COMMENTS_KEY, cacheManager = "postCacheManager")
    public Long addComment(final Long postId, final CommentRequest commentRequest, final MemberAuth memberAuth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        Member member = authorizationHelper.getMember(memberAuth);

        String sanitized = HtmlSanitizer.sanitize(commentRequest.content());
        Comment comment = Comment.parent(sanitized, member, post);
        post.addComment(comment);
        commentRepository.save(comment);
        popularScoreService.applyComment(postId);
        return comment.getId();
    }

    @Cacheable(cacheNames = CacheKey.COMMENTS, key = CacheKey.COMMENTS_KEY, cacheManager = "postCacheManager")
    public CommentResponse findComments(final Long postId) {
        List<Comment> comments = commentRepository.findCommentsByPostIdWithMember(postId);

        return CommentResponse.createByComments(comments);
    }

    @Transactional
    public void editComment(final Long id, final CommentRequest commentRequest, final MemberAuth memberAuth) {
        Comment comment = findActiveCommentOrThrow(id);
        authorizationHelper.validateIsAuthor(comment.getMember(), authorizationHelper.getMember(memberAuth));
        comment.updateContent(HtmlSanitizer.sanitize(commentRequest.content()));
        evictComments(comment.getPost().getId());
    }

    @Transactional
    public void deleteComment(final Long id, final MemberAuth memberAuth) {
        Comment comment = findActiveCommentOrThrow(id);
        authorizationHelper.validateIsAuthor(comment.getMember(), authorizationHelper.getMember(memberAuth));
        popularScoreService.cancelComment(comment.getPost().getId());
        comment.softDelete();
        evictComments(comment.getPost().getId());
    }

    private void evictComments(final Long postId) {
        Cache cache = cacheManager.getCache(CacheKey.COMMENTS);
        if (cache != null) {
            cache.evict(postId);
        }
    }

    private Comment findActiveCommentOrThrow(final Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new FretBoardException(ExceptionType.COMMENT_NOT_FOUND);
        }
        return comment;
    }

}
