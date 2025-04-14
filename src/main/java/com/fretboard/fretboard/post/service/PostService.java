package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.service.BoardService;
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
    private final BoardService boardService;

    @Transactional
    public Long addPost(final Long boardId, final PostRequest postRequest) {
        Post savedPost = postRepository.save(postRequest.toPost());
        boardService.savePostBoard(savedPost, boardId);
        return savedPost.getId();
    }

    @Transactional
    public void updatePost(Long id, PostRequest postRequest) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        post.setTitle(postRequest.title());
        post.setContent(postRequest.content());
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        postRepository.delete(post);
    }

    public PostDetailResponse findPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostDetailResponse.of(post);
    }
}
