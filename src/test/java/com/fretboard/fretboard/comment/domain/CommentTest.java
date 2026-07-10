package com.fretboard.fretboard.comment.domain;

import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("softDelete_호출_시_deletedAt_필드가_설정됨")
    void softDelete_호출_시_deletedAt_필드가_설정됨() {
        // given
        Comment comment = Comment.parent("댓글 내용", testMember(), null);

        // when
        comment.softDelete();

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("소프트_딜리트된_댓글은_isDeleted_true를_반환함")
    void 소프트_딜리트된_댓글은_isDeleted_true를_반환함() {
        // given
        Comment comment = Comment.parent("댓글 내용", testMember(), null);

        // when
        comment.softDelete();

        // then
        assertThat(comment.isDeleted()).isTrue();
    }
}
