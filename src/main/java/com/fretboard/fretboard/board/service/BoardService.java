package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.dto.BoardRequest;
import com.fretboard.fretboard.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public Long addBoard(final BoardRequest boardRequest) {
        Board saveBoard = boardRepository.save(boardRequest.toBoard());
        return saveBoard.getId();
    }
}
