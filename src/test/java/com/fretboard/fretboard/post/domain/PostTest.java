package com.fretboard.fretboard.post.domain;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    private Member testMember() {
        return Member.builder()
                .username("user1")
                .password("pass")
                .nickname("nick1")
                .role(Role.USER)
                .build();
    }

    private Board testBoard() {
        return Board.builder()
                .title("자유게시판")
                .description("설명")
                .slug("free")
                .boardType(BoardType.WRITABLE)
                .build();
    }

    @Test
    @DisplayName("post.edit() 호출 시 title과 content가 변경된다")
    void edit_changesTitleAndContent() {
        // given
        Post post = Post.builder()
                .title("원래 제목")
                .content("원래 내용")
                .member(testMember())
                .board(testBoard())
                .build();

        // when
        post.edit("새 제목", "새 내용");

        // then
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 내용");
    }
}
