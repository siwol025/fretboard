package com.fretboard.fretboard.board.dto.request;

import com.fretboard.fretboard.board.domain.Board;
import jakarta.validation.constraints.NotBlank;

public record BoardRequest(
        @NotBlank
        String title
) {
    public Board toBoard() {
        return Board.builder()
                .title(title)
                .build();
    }
}
