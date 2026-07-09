package com.fretboard.fretboard.board.service;

import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fretboard.fretboard.board.domain.Board;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private AuthorizationHelper authorizationHelper;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("일반 회원이 게시판을 생성하려 하면 FORBIDDEN 예외가 발생한다")
    void createBoard_일반회원이면_FORBIDDEN_예외() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member userMember = Member.builder()
                .username("user")
                .password("password")
                .nickname("일반유저")
                .role(Role.USER)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(userMember);

        BoardRequest request = new BoardRequest("테스트 게시판", "설명", "test-board", BoardType.WRITABLE);

        // when & then
        assertThatThrownBy(() -> boardService.createBoard(request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(e -> {
                    FretBoardException fretBoardException = (FretBoardException) e;
                    assertThat(fretBoardException.getExceptionType()).isEqualTo(ExceptionType.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("일반 회원이 게시판을 수정하려 하면 FORBIDDEN 예외가 발생한다")
    void editBoard_일반회원이면_FORBIDDEN_예외() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member userMember = Member.builder()
                .username("user")
                .password("password")
                .nickname("일반유저")
                .role(Role.USER)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(userMember);

        BoardRequest request = new BoardRequest("테스트 게시판", "설명", "test-board", BoardType.WRITABLE);

        // when & then
        assertThatThrownBy(() -> boardService.editBoard(1L, request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(e -> {
                    FretBoardException fretBoardException = (FretBoardException) e;
                    assertThat(fretBoardException.getExceptionType()).isEqualTo(ExceptionType.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("일반 회원이 게시판을 삭제하려 하면 FORBIDDEN 예외가 발생한다")
    void deleteBoard_일반회원이면_FORBIDDEN_예외() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member userMember = Member.builder()
                .username("user")
                .password("password")
                .nickname("일반유저")
                .role(Role.USER)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(userMember);

        // when & then
        assertThatThrownBy(() -> boardService.deleteBoard(1L, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(e -> {
                    FretBoardException fretBoardException = (FretBoardException) e;
                    assertThat(fretBoardException.getExceptionType()).isEqualTo(ExceptionType.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("관리자가 게시판을 생성하면 예외 없이 성공한다")
    void createBoard_관리자이면_성공() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member adminMember = Member.builder()
                .username("admin")
                .password("password")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();

        BoardRequest request = new BoardRequest("테스트 게시판", "설명", "test-board", BoardType.WRITABLE);
        Board savedBoard = Board.builder()
                .title(request.title())
                .description(request.description())
                .slug(request.slug())
                .boardType(request.boardType())
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(adminMember);
        given(boardRepository.save(any(Board.class))).willReturn(savedBoard);

        // when & then
        assertThatCode(() -> boardService.createBoard(request, memberAuth))
                .doesNotThrowAnyException();
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    @DisplayName("관리자가 게시판을 수정하면 예외 없이 성공한다")
    void editBoard_관리자이면_성공() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member adminMember = Member.builder()
                .username("admin")
                .password("password")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();

        BoardRequest request = new BoardRequest("수정된 게시판", "수정된 설명", "edited-board", BoardType.WRITABLE);
        Board existingBoard = Board.builder()
                .title("원래 게시판")
                .description("원래 설명")
                .slug("original-board")
                .boardType(BoardType.WRITABLE)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(adminMember);
        given(boardRepository.findById(1L)).willReturn(Optional.of(existingBoard));

        // when & then
        assertThatCode(() -> boardService.editBoard(1L, request, memberAuth))
                .doesNotThrowAnyException();
        verify(boardRepository).findById(1L);
    }

    @Test
    @DisplayName("관리자가 게시판을 삭제하면 예외 없이 성공한다")
    void deleteBoard_관리자이면_성공() {
        // given
        MemberAuth memberAuth = new MemberAuth(1L);
        Member adminMember = Member.builder()
                .username("admin")
                .password("password")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();

        Board existingBoard = Board.builder()
                .title("삭제할 게시판")
                .description("설명")
                .slug("delete-board")
                .boardType(BoardType.WRITABLE)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(adminMember);
        given(boardRepository.findById(1L)).willReturn(Optional.of(existingBoard));

        // when & then
        assertThatCode(() -> boardService.deleteBoard(1L, memberAuth))
                .doesNotThrowAnyException();
        verify(boardRepository).delete(existingBoard);
    }
}
