package com.fretboard.fretboard.board.dto.response;

import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
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
    public static List<RecentPostsPerBoardResponse> of(List<PostBoard> postBoards) {
        return postBoards.stream()
                .collect(Collectors.groupingBy(
                        pb -> new BoardKey(pb.getBoard().getId(), pb.getBoard().getTitle(), pb.getBoard().getSlug()),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                pb -> PostSummaryResponse.of(pb.getPost()),
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


