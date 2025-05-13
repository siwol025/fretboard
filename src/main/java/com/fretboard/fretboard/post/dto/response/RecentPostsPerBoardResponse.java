package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.domain.Post;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record RecentPostsPerBoardResponse(
        Long boardId,
        String boardTitle,
        String boardSlug,
        List<PostSummaryResponse> posts
) {
    public static List<RecentPostsPerBoardResponse> of(List<Post> posts) {
        return posts.stream()
                .collect(Collectors.groupingBy(
                        p -> new BoardKey(p.getBoard().getId(), p.getBoard().getTitle(), p.getBoard().getSlug()),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                PostSummaryResponse::of,
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(entry -> RecentPostsPerBoardResponse.builder()
                        .boardId(entry.getKey().boardId())
                        .boardTitle(entry.getKey().boardTitle())
                        .boardSlug(entry.getKey().boardSlug())
                        .posts(entry.getValue())
                        .build()
                )
                .toList();
    }

    public record BoardKey(Long boardId, String boardTitle, String boardSlug) { }
}


