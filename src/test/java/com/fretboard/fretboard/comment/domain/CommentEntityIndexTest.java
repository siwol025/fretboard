package com.fretboard.fretboard.comment.domain;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CommentEntityIndexTest {

    @Test
    @DisplayName("Comment_post_id_인덱스_선언됨")
    void Comment_post_id_인덱스_선언됨() {
        Table table = Comment.class.getAnnotation(Table.class);
        assertThat(table)
                .as("Comment 엔티티에 @Table 어노테이션이 없습니다.")
                .isNotNull();

        Set<String> indexedColumns = Arrays.stream(table.indexes())
                .map(Index::columnList)
                .collect(Collectors.toSet());

        assertThat(indexedColumns)
                .as("post_id 인덱스가 선언되어 있지 않습니다. 실제 인덱스: %s", indexedColumns)
                .contains("post_id");

        assertThat(indexedColumns)
                .as("member_id 인덱스가 선언되어 있지 않습니다. 실제 인덱스: %s", indexedColumns)
                .contains("member_id");
    }
}
