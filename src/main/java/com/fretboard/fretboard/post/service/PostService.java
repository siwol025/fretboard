package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostRequest;
import com.fretboard.fretboard.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public Long addPost(PostRequest postRequest) {
        Post savedPost = postRepository.save(postRequest.toPost());
        return savedPost.getId();
    }
}
