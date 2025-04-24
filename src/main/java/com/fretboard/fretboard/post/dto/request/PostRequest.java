package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.post.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostRequest(
        @NotNull
        Long boardId,

        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        String content
) {
    public Post toPost() {
        return Post.builder().
                title(title).
                content(content).
                build();
    }
}
