package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.post.domain.Post;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentPostsPerBoardResponse {
    Long boardId;
    String boardTitle;
    String boardSlug;
    List<PostSummaryResponse> posts;

    @Builder
    public RecentPostsPerBoardResponse(Long boardId, String boardTitle, String boardSlug,
                                       List<PostSummaryResponse> posts) {
        this.boardId = boardId;
        this.boardTitle = boardTitle;
        this.boardSlug = boardSlug;
        this.posts = posts;
    }

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
                        .boardId(entry.getKey().getBoardId())
                        .boardTitle(entry.getKey().getBoardTitle())
                        .boardSlug(entry.getKey().getBoardSlug())
                        .posts(entry.getValue())
                        .build()
                )
                .toList();
    }
}