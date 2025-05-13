package com.fretboard.fretboard.comment.dto.response;

import com.fretboard.fretboard.comment.domain.Comment;
import java.util.List;

public record CommentResponse(
        List<CommentDetailResponse> contents
) {
    public static CommentResponse createByComments(List<Comment> comments) {
        List<CommentDetailResponse> commentDetailResponses = comments.stream()
                .map(CommentDetailResponse::from)
                .toList();
        return new CommentResponse(commentDetailResponses);
    }
}
