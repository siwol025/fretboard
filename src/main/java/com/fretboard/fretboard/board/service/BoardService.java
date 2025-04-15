package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.board.dto.BoardRequest;
import com.fretboard.fretboard.board.dto.BoardsElementResponse;
import com.fretboard.fretboard.board.dto.BoardsResponse;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.board.repository.PostBoardRepository;
import com.fretboard.fretboard.exception.ExceptionType;
import com.fretboard.fretboard.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final PostBoardRepository postBoardRepository;

    @Transactional
    public Long createBoard(final BoardRequest boardRequest) {
        Board saveBoard = boardRepository.save(boardRequest.toBoard());
        return saveBoard.getId();
    }

    @Transactional
    public void editBoardTitle(final Long id, final BoardRequest boardRequest) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUNT));
        board.setTitle(boardRequest.title());
    }

    @Transactional
    public void deleteBoard(final Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUNT));
        boardRepository.delete(board);
    }

    @Transactional
    public void savePostBoard(final Post savedPost, final Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUNT));

        PostBoard postBoard = PostBoard.of(savedPost, board);
        savedPost.addPostBoard(postBoard);
        board.addPostBoard(postBoard);

        postBoardRepository.save(postBoard);
    }

    public BoardsResponse findBoards() {
        List<BoardsElementResponse> contents = boardRepository.findAll().stream()
                .map(BoardsElementResponse::from)
                .toList();
        return new BoardsResponse(contents);
    }
}