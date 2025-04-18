package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.post.dto.PostDetailResponse;
import com.fretboard.fretboard.post.dto.PostRequest;
import com.fretboard.fretboard.post.dto.PostResponse;
import com.fretboard.fretboard.post.service.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping("/boards/{boardId}/posts")
    public ResponseEntity<Void> addPost(@PathVariable Long boardId,
                                        @Valid @RequestBody PostRequest postRequest) {
        Long postId = postService.addPost(boardId, postRequest);
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    @GetMapping("/boards/{boardId}/posts")
    public ResponseEntity<PostResponse> findPostsByBoardId(@PathVariable Long boardId,
                                                           @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PostResponse postResponse = postService.findPostsByBoardId(boardId, pageable);
        return ResponseEntity.ok().body(postResponse);
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostDetailResponse> findPost(@PathVariable Long id) {
        return ResponseEntity.ok().body(postService.findPost(id));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @Valid @RequestBody PostRequest postRequest) {
        postService.updatePost(id, postRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
