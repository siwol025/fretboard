package com.fretboard.fretboard.board.controller;

import com.fretboard.fretboard.board.dto.BoardRequest;
import com.fretboard.fretboard.board.service.BoardService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {
    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<Long> createBoard(@Valid @RequestBody BoardRequest boardRequest) {
        Long boardId = boardService.addBoard(boardRequest);
        return ResponseEntity.created(URI.create("/board/" + boardId)).build();
    }
}
