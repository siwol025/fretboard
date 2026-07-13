package com.fretboard.fretboard.post.domain;

import com.fretboard.fretboard.global.entity.BaseEntity;
import com.fretboard.fretboard.member.domain.Member;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "idx_post_like_post_id", columnList = "post_id"),
        @Index(name = "idx_post_like_member_id", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public PostLike(Long id, Post post, Member member) {
        this.id = id;
        this.post = post;
        this.member = member;
    }

    public static PostLike of(Post post, Member member) {
        return PostLike.builder()
                .post(post)
                .member(member)
                .build();
    }
}
