package com.fretboard.fretboard.board.dto;

import java.util.List;

public record BoardListResponse(
        List<BoardElementResponse> contents
) {
}
