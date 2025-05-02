package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.PostBoard;
import com.fretboard.fretboard.board.repository.PostBoardRepository;
import com.fretboard.fretboard.board.service.BoardService;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
import com.fretboard.fretboard.post.dto.request.PostRequest;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final PostBoardRepository postBoardRepository;
    private final BoardService boardService;

    @Transactional
    public Long addPost(final PostRequest request) {
        Post savedPost = postRepository.save(request.toPost());
        boardService.savePostBoard(savedPost, request.boardId());
        return savedPost.getId();
    }

    @Transactional
    public void updatePost(final Long id, final PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        post.setTitle(request.title());
        post.setContent(request.content());
    }

    @Transactional
    public void deletePost(final Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        postRepository.delete(post);
    }

    public PostDetailResponse findPost(final Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new FretBoardException(ExceptionType.POST_NOT_FOUND));
        return PostDetailResponse.of(post);
    }

    public PostListResponse findPostsByBoardId(final Long boardId, Pageable pageable) {
        Page<PostBoard> postBoardPage = postBoardRepository.findPostBoardsByBoardId(boardId, pageable);
        return PostListResponse.of(postBoardPage);
    }

    public List<PostSummaryResponse> findPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.stream()
                .map(PostSummaryResponse::of)
                .toList();
    }
}
