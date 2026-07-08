package com.fretboard.fretboard.board.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTest {

    @Test
    @DisplayName("board.update() 호출 시 title, description, slug, boardType이 변경된다")
    void update_changesAllFields() {
        // given
        Board board = Board.builder()
                .title("원래 제목")
                .description("원래 설명")
                .slug("original-slug")
                .boardType(BoardType.WRITABLE)
                .build();

        // when
        board.update("새 제목", "새 설명", "new-slug", BoardType.NON_WRITABLE);

        // then
        assertEquals("새 제목", board.getTitle());
        assertEquals("새 설명", board.getDescription());
        assertEquals("new-slug", board.getSlug());
        assertEquals(BoardType.NON_WRITABLE, board.getBoardType());
    }
}
