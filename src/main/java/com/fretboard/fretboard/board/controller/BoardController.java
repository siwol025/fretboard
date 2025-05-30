package com.fretboard.fretboard.board.controller;

import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.dto.response.BoardListResponse;
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
                                            @Valid @RequestBody BoardRequest request) {
        Long boardId = boardService.createBoard(request);
        return ResponseEntity.created(URI.create("/board/" + boardId)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editBoard(@PathVariable Long id,
                                               @LoginMember MemberAuth memberAuth,
                                               @Valid @RequestBody BoardRequest request) {
        boardService.editBoard(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<BoardListResponse> findBoardsContents() {
        BoardListResponse response = boardService.findBoards();
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardElementResponse> findBoard(@PathVariable Long id) {
        BoardElementResponse response = boardService.findBoard(id);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id,
                                            @LoginMember MemberAuth memberAuth) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
