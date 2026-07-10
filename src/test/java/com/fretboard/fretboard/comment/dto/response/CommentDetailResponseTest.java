package com.fretboard.fretboard.comment.dto.response;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentDetailResponseTest {

    private Member createMember() {
        return Member.builder()
                .username("u")
                .password("p")
                .nickname("nick")
                .role(Role.USER)
                .build();
    }

    @Test
    void 삭제된_댓글은_content가_삭제된_댓글입니다로_반환됨() {
        Member member = createMember();
        Comment comment = Comment.parent("원본 내용", member, null);
        comment.softDelete();

        CommentDetailResponse response = CommentDetailResponse.from(comment);

        assertThat(response.content()).isEqualTo(CommentDetailResponse.DELETED_CONTENT);
    }

    @Test
    void 삭제되지_않은_댓글은_원본_content가_반환됨() {
        Member member = createMember();
        Comment comment = Comment.parent("원본 내용", member, null);

        CommentDetailResponse response = CommentDetailResponse.from(comment);

        assertThat(response.content()).isEqualTo("원본 내용");
    }
}
