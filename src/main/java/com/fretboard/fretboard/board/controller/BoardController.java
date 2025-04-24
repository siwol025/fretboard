package com.fretboard.fretboard.board.controller;

import com.fretboard.fretboard.board.dto.BoardRequest;
import com.fretboard.fretboard.board.dto.BoardsResponse;
import com.fretboard.fretboard.board.service.BoardService;
import com.fretboard.fretboard.global.auth.annotation.LoginMember;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {
    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<Long> createBoard(@LoginMember MemberAuth memberAuth,
                                            @Valid @RequestBody BoardRequest boardRequest) {
        Long boardId = boardService.createBoard(boardRequest);
        return ResponseEntity.created(URI.create("/board/" + boardId)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editBoardTitle(@PathVariable Long id,
                                               @LoginMember MemberAuth memberAuth,
                                               @Valid @RequestBody BoardRequest boardRequest) {
        boardService.editBoardTitle(id, boardRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<BoardsResponse> findBoardsContents() {
        BoardsResponse boardsResponse = boardService.findBoards();
        return ResponseEntity.ok().body(boardsResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id,
                                            @LoginMember MemberAuth memberAuth) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
