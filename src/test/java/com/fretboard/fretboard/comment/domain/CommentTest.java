package com.fretboard.fretboard.comment.domain;

import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentTest {

    private Member testMember() {
        return Member.builder()
                .username("user1")
                .password("pass")
                .nickname("nick1")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("comment.updateContent() 호출 시 content가 변경된다")
    void updateContent_changesContent() {
        // given
        Comment comment = Comment.parent("원래 내용", testMember(), null);

        // when
        comment.updateContent("수정된 내용");

        // then
        assertEquals("수정된 내용", comment.getContent());
    }
}
