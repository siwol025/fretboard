package com.fretboard.fretboard.board.domain;

import com.fretboard.fretboard.global.entity.BaseEntity;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(unique = true, nullable = false, length = 50)
    private String slug;

    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    @Builder
    public Board(String title, String description, String slug, BoardType boardType) {
        this.title = title;
        this.description = description;
        this.slug = slug;
        this.boardType = boardType;
    }

    public void validateWritable(Member member) {
        if (!this.boardType.canWrite(member)) {
            throw new FretBoardException(ExceptionType.FORBIDDEN_WRITE_PERMISSION);
        }
    }
}
