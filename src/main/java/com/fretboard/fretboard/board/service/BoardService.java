package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.dto.response.BoardListResponse;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.common.CacheKey;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    @CacheEvict(cacheNames = CacheKey.BOARDS, key = CacheKey.BOARDS_KEY, cacheManager = CacheKey.CACHE_MANAGER)
    public Long createBoard(final BoardRequest request, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board saveBoard = boardRepository.save(request.toBoard());
        return saveBoard.getId();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheKey.BOARDS, key = CacheKey.BOARDS_KEY, cacheManager = CacheKey.CACHE_MANAGER),
            @CacheEvict(cacheNames = CacheKey.BOARD, key = CacheKey.BOARD_KEY, cacheManager = CacheKey.CACHE_MANAGER)
    })
    public void editBoard(final Long id, final BoardRequest request, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board board = findBoardEntity(id);
        board.update(request.title(), request.description(), request.slug(), request.boardType());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheKey.BOARDS, key = CacheKey.BOARDS_KEY, cacheManager = CacheKey.CACHE_MANAGER),
            @CacheEvict(cacheNames = CacheKey.BOARD, key = CacheKey.BOARD_KEY, cacheManager = CacheKey.CACHE_MANAGER)
    })
    public void deleteBoard(final Long id, final MemberAuth memberAuth) {
        requireAdmin(memberAuth);
        Board board = findBoardEntity(id);
        boardRepository.delete(board);
    }

    private void requireAdmin(final MemberAuth memberAuth) {
        Member member = authorizationHelper.getMember(memberAuth);
        if (!member.isAdmin()) {
            throw new FretBoardException(ExceptionType.FORBIDDEN);
        }
    }

    @Cacheable(cacheNames = CacheKey.BOARDS, key = CacheKey.BOARDS_KEY, cacheManager = CacheKey.CACHE_MANAGER)
    public BoardListResponse findBoards() {
        List<BoardElementResponse> contents = boardRepository.findAll().stream()
                .map(BoardElementResponse::of)
                .toList();
        return new BoardListResponse(contents);
    }

    @Cacheable(cacheNames = CacheKey.BOARD, key = CacheKey.BOARD_KEY, cacheManager = CacheKey.CACHE_MANAGER)
    public BoardElementResponse findBoard(final Long id) {
        return BoardElementResponse.of(findBoardEntity(id));
    }

    private Board findBoardEntity(final Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.BOARD_NOT_FOUND));
    }
}
