package com.fretboard.fretboard.board.dto.request;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardRequest(
        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 50, message = "0 ~ 50자 사이의 값을 입력해주세요.")
        String slug,

        @NotBlank
        BoardType boardType
) {
    public Board toBoard() {
        return Board.builder()
                .title(title)
                .description(description)
                .slug(slug)
                .boardType(boardType)
                .build();
    }
}
