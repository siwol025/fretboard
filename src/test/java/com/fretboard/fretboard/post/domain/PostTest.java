package com.fretboard.fretboard.post.domain;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import java.time.LocalDateTime;
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

    @Test
    @DisplayName("promote 호출 시 popularAt이 설정되고 isPopular가 true가 된다")
    void promote_호출시_popularAt_설정되고_isPopular_true() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .member(testMember())
                .board(testBoard())
                .build();

        // then: 새 글은 인기글이 아니다
        assertThat(post.isPopular()).isFalse();
        assertThat(post.getPopularAt()).isNull();

        // when
        post.promote();

        // then
        assertThat(post.isPopular()).isTrue();
        assertThat(post.getPopularAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 승격된 글은 재승격 시 popularAt이 유지된다")
    void 이미_승격된_글은_재승격시_popularAt_유지() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .member(testMember())
                .board(testBoard())
                .build();
        post.promote();
        LocalDateTime firstPopularAt = post.getPopularAt();

        // when
        post.promote();

        // then
        assertThat(post.getPopularAt()).isEqualTo(firstPopularAt);
    }
}
