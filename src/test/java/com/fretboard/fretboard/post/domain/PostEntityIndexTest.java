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
    @DisplayName("Post_board_id_인덱스_선언됨")
    void Post_board_id_인덱스_선언됨() {
        Table table = Post.class.getAnnotation(Table.class);
        assertThat(table)
                .as("Post 엔티티에 @Table 어노테이션이 없습니다.")
                .isNotNull();

        Set<String> indexedColumns = Arrays.stream(table.indexes())
                .map(Index::columnList)
                .collect(Collectors.toSet());

        assertThat(indexedColumns)
                .as("board_id 인덱스가 선언되어 있지 않습니다. 실제 인덱스: %s", indexedColumns)
                .contains("board_id");

        assertThat(indexedColumns)
                .as("member_id 인덱스가 선언되어 있지 않습니다. 실제 인덱스: %s", indexedColumns)
                .contains("member_id");

        assertThat(indexedColumns)
                .as("created_at 인덱스가 선언되어 있지 않습니다. 실제 인덱스: %s", indexedColumns)
                .contains("created_at");
    }
}
