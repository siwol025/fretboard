package com.fretboard.fretboard.comment.domain;

import com.fretboard.fretboard.global.entity.BaseEntity;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "idx_comment_post_id", columnList = "post_id"),
        @Index(name = "idx_comment_member_id", columnList = "member_id")
})
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Builder
    public Comment(String content, Post post, Member member, Comment parent) {
        this.content = content;
        this.post = post;
        this.member = member;
        this.parent = parent;
    }

    public void updateContent(final String content) {
        this.content = content;
    }

    public static Comment parent(String content, Member member, Post post) {
        return Comment.builder()
                .content(content)
                .post(post)
                .member(member)
                .build();
    }
}
