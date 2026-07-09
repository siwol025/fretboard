package com.fretboard.fretboard.board.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(board.getTitle()).isEqualTo("새 제목");
        assertThat(board.getDescription()).isEqualTo("새 설명");
        assertThat(board.getSlug()).isEqualTo("new-slug");
        assertThat(board.getBoardType()).isEqualTo(BoardType.NON_WRITABLE);
    }
}
