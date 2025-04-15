package com.fretboard.fretboard.comment.service;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.CommentRequest;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.exception.ExceptionType;
import com.fretboard.fretboard.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
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
    public Long addComment(Long postId, CommentRequest commentRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));

        Comment comment = Comment.parent(commentRequest.content(), post);
        post.addComment(comment);
        commentRepository.save(comment);

        return comment.getId();
    }
}
