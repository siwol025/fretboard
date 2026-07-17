package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCountLoader {

    private final CommentRepository commentRepository;

    public Map<Long, Long> load(final List<Long> postIds) {
        return commentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostCommentCountDto::postId, PostCommentCountDto::commentCount));
    }
}
