package com.fretboard.fretboard.post.dto.response;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.post.domain.Post;
import java.util.List;
import java.util.Map;
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

    public static RecentPostsPerBoardResponse of(Board board, List<Post> posts, Map<Long, Long> commentCountMap) {
        List<PostSummaryResponse> postSummaries = posts.stream()
                .map(post -> PostSummaryResponse.of(
                        post,
                        commentCountMap.getOrDefault(post.getId(), 0L).intValue()))
                .toList();

        return RecentPostsPerBoardResponse.builder()
                .boardId(board.getId())
                .boardTitle(board.getTitle())
                .boardSlug(board.getSlug())
                .posts(postSummaries)
                .build();
    }
}
