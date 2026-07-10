package com.fretboard.fretboard.post.dto.request;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostNewRequest(
        @NotNull
        Long boardId,

        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        @Size(max = 10_000, message = "본문은 10,000자를 초과할 수 없습니다.")
        String content
) {
    public Post toPost(final Member member, final Board board, final String convertedContent) {
        return Post.builder()
                .title(title)
                .content(convertedContent)
                .member(member)
                .board(board)
                .build();
    }
}