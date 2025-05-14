package com.fretboard.fretboard.board.dto.response;

import com.fretboard.fretboard.board.domain.Board;
import lombok.Builder;

@Builder
public record BoardElementResponse(
        Long id,
        String title,
        String description
) {
    public static BoardElementResponse of(Board board) {
        return BoardElementResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .description(board.getDescription())
                .build();
    }
}
