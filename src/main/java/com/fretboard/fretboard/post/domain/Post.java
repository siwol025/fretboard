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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(indexes = {
        @Index(name = "idx_post_board_created_id", columnList = "board_id, created_at, id"),
        @Index(name = "idx_post_member_created_id", columnList = "member_id, created_at, id")
})
// like_count/view_count 는 bulk @Modifying UPDATE 로 갱신되어 managed 엔티티 스냅샷과 어긋난다.
// @DynamicUpdate 없이 promote()/edit() 의 dirty flush 가 발생하면 전체 컬럼 UPDATE 가 stale 한
// like_count 스냅샷을 되써서 bulk 증가분을 덮어버린다. 변경된 컬럼만 UPDATE 하도록 하여 방지한다.
@DynamicUpdate
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

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(name = "popular_at")
    private LocalDateTime popularAt;

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

    public boolean isPopular() {
        return popularAt != null;
    }

    public void promote(LocalDateTime now) {
        if (popularAt == null) {
            popularAt = now;
        }
    }
}
