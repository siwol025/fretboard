package com.fretboard.fretboard.comment.service;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.helper.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public Long addComment(final Long postId, final CommentRequest commentRequest, final MemberAuth memberAuth) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        Member member = authorizationHelper.getMember(memberAuth);

        Comment comment = Comment.parent(commentRequest.content(), member, post);
        post.addComment(comment);
        commentRepository.save(comment);
        return comment.getId();
    }

    public CommentResponse findComments(final Long postId) {
        List<Comment> comments = commentRepository.findCommentsByPostIdWithMember(postId);

        return CommentResponse.createByComments(comments);
    }

    @Transactional
    public void editComment(final Long id, final CommentRequest commentRequest, final MemberAuth memberAuth) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.COMMENT_NOT_FOUND));

        authorizationHelper.validateIsAuthor(comment.getMember(), authorizationHelper.getMember(memberAuth));
        comment.updateContent(commentRequest.content());
    }

    @Transactional
    public void deleteComment(final Long id, final MemberAuth memberAuth) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.COMMENT_NOT_FOUND));
        authorizationHelper.validateIsAuthor(comment.getMember(), authorizationHelper.getMember(memberAuth));
        commentRepository.delete(comment);
    }

}
