package com.fretboard.fretboard.post.dto;

import com.fretboard.fretboard.post.domain.Post;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 게시글 상세의 정적(static) 본문만 담는 DTO.
 *
 * <p>조회수·좋아요 수·isLiked·댓글 등 동적 데이터는 포함하지 않는다.
 * 이 정적 본문만 {@code postDetail} 캐시에 적재되고, 동적 데이터는 매 조회마다 합성한다.
 */
public record PostStaticView(
        Long id,
        String title,
        String content,
        Long authorId,
        String author,
        LocalDateTime createdAt,
        Long boardId,
        String boardTitle,
        // 최초 접근 시 조회수 카운터를 시딩하기 위한 기준값(seed). 실시간 조회수가 아니다.
        Long baseViewCount
) implements Serializable {

    public static PostStaticView from(Post post) {
        return new PostStaticView(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getId(),
                post.getMember().getNickname(),
                post.getCreatedAt(),
                post.getBoard().getId(),
                post.getBoard().getTitle(),
                post.getViewCount()
        );
    }
}
