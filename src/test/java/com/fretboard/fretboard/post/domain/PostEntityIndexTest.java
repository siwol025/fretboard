package com.fretboard.fretboard.post.domain;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PostEntityIndexTest {

    @Test
    @DisplayName("Post_커버링_복합_인덱스_선언됨")
    void Post_커버링_복합_인덱스_선언됨() {
        Table table = Post.class.getAnnotation(Table.class);
        assertThat(table)
                .as("Post 엔티티에 @Table 어노테이션이 없습니다.")
                .isNotNull();

        Set<String> indexColumnLists = Arrays.stream(table.indexes())
                .map(Index::columnList)
                .collect(Collectors.toSet());

        assertThat(indexColumnLists)
                .as("게시판 목록 커버링 복합 인덱스(board_id, created_at, id)가 선언되어야 한다. 실제: %s", indexColumnLists)
                .contains("board_id, created_at, id");

        assertThat(indexColumnLists)
                .as("내 게시글 커버링 복합 인덱스(member_id, created_at, id)가 선언되어야 한다. 실제: %s", indexColumnLists)
                .contains("member_id, created_at, id");
    }
}
