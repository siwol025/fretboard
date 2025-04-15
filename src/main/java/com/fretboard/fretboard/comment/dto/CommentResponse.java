package com.fretboard.fretboard.comment.dto;

import java.util.List;

public record CommentResponse(
        List<CommentDetailResponse> contents
) {
}
