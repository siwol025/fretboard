package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.dto.response.BoardListResponse;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.helper.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public Long createBoard(final BoardRequest request, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board saveBoard = boardRepository.save(request.toBoard());
        return saveBoard.getId();
    }

    @Transactional
    public void editBoard(final Long id, final BoardRequest request, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        board.update(request.title(), request.description(), request.slug(), request.boardType());
    }

    @Transactional
    public void deleteBoard(final Long id, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
        boardRepository.delete(board);
    }

    private void requireAdmin(final MemberAuth memberAuth) {
        Member member = authorizationHelper.getMember(memberAuth);
        if (!member.isAdmin()) {
            throw new FretBoardException(ExceptionType.FORBIDDEN);
        }
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