package com.fretboard.fretboard.board.dto.response;

import java.util.List;

public record BoardListResponse(
        List<BoardElementResponse> contents
) {
}
