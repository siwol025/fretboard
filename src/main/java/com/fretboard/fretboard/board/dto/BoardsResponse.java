package com.fretboard.fretboard.board.dto;

import java.util.List;

public record BoardsResponse(
        List<BoardsElementResponse> contents
) {
}
