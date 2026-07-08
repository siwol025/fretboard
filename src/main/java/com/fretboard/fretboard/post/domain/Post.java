package com.fretboard.fretboard.post.domain;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.global.entity.BaseEntity;
import com.fretboard.fretboard.member.domain.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "idx_post_board_id", columnList = "board_id"),
        @Index(name = "idx_post_member_id", columnList = "member_id"),
        @Index(name = "idx_post_created_at", columnList = "created_at")
})
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder
    public Post(String title, String content, Member member, Board board, Long viewCount) {
        this.title = title;
        this.content = content;
        this.member = member;
        this.board = board;
        this.viewCount = (viewCount != null) ? viewCount : 0L;
    }

    public void edit(final String title, final String content) {
        this.title = title;
        this.content = content;
    }

    public void addComment(final Comment comment) {
        comments.add(comment);
    }
}
