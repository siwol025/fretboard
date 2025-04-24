package com.fretboard.fretboard.comment.service;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.CommentRequest;
import com.fretboard.fretboard.comment.dto.CommentResponse;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
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

    @Transactional
    public Long addComment(final Long postId, final CommentRequest commentRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        Comment comment = Comment.parent(commentRequest.content(), post);
        post.addComment(comment);
        commentRepository.save(comment);

        return comment.getId();
    }

    public CommentResponse findComments(final Long postId) {
        List<Comment> comments = commentRepository.findCommentsByPostId(postId);

        return CommentResponse.createByComments(comments);
    }

    @Transactional
    public void editComment(final Long id, final CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.COMMENT_NOT_FOUND));

        comment.setContent(commentRequest.content());
    }

    @Transactional
    public void deleteComment(final Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.COMMENT_NOT_FOUND));
        commentRepository.delete(comment);
    }
}
