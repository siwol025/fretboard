package com.fretboard.fretboard.post.dto;

import java.time.LocalDateTime;

public interface PostSearchResultProjection {
    Long getId();
    String getTitle();
    String getAuthor();
    Long getBoardId();
    String getBoardTitle();
    LocalDateTime getCreatedAt();
    Long getViewCount();
}
