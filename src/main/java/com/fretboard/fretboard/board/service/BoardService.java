package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.dto.response.BoardListResponse;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public Long createBoard(final BoardRequest request) {
        Board saveBoard = boardRepository.save(request.toBoard());
        return saveBoard.getId();
    }

    @Transactional
    public void editBoard(final Long id, final BoardRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        board.setTitle(request.title());
        board.setDescription(request.description());
        board.setSlug(request.slug());
        board.setBoardType(request.boardType());
    }

    @Transactional
    public void deleteBoard(final Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        boardRepository.delete(board);
    }

    public BoardListResponse findBoards() {
        List<BoardElementResponse> contents = boardRepository.findAll().stream()
                .map(BoardElementResponse::of)
                .toList();
        return new BoardListResponse(contents);
    }

    public BoardElementResponse findBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        return BoardElementResponse.of(board);
    }
}