package com.fretboard.fretboard.post.dto.request;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostNewRequest(
        @NotNull
        Long boardId,

        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        String content
) {
    public Post toPost(final Member member, final Board board) {
        return Post.builder()
                .title(title)
                .content(content)
                .member(member)
                .board(board)
                .build();
    }
}