package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.post.domain.Post;
import jakarta.validation.constraints.NotBlank;

public record PostRequest(
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
