package com.fretboard.fretboard.board.dto.response;

import com.fretboard.fretboard.board.domain.Board;
import lombok.Builder;

public record BoardElementResponse(
        Long id,
        String title
) {
    @Builder
    public BoardElementResponse(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public static BoardElementResponse from(Board board) {
        return BoardElementResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .build();
    }
}
