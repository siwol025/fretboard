package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.exception.ExceptionType;
import com.fretboard.fretboard.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostDetailResponse;
import com.fretboard.fretboard.post.dto.PostRequest;
import com.fretboard.fretboard.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public Long addPost(PostRequest postRequest) {
        Post savedPost = postRepository.save(postRequest.toPost());
        return savedPost.getId();
    }

    public PostDetailResponse findPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostDetailResponse.of(post);
    }
}
