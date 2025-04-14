package com.fretboard.fretboard.board.dto;

import com.fretboard.fretboard.board.domain.Board;
import lombok.Builder;

public record BoardsElementResponse(
        Long id,
        String title
) {
    @Builder
    public BoardsElementResponse(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public static BoardsElementResponse from(Board board) {
        return BoardsElementResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .build();
    }
}
