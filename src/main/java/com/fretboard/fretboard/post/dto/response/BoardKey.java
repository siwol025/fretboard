package com.fretboard.fretboard.post.dto.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class BoardKey {
    Long boardId;
    String boardTitle;
    String boardSlug;

    public BoardKey(Long boardId, String boardTitle, String boardSlug) {
        this.boardId = boardId;
        this.boardTitle = boardTitle;
        this.boardSlug = boardSlug;
    }
}
