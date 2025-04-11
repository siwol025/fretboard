package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.post.dto.PostDetailResponse;
import com.fretboard.fretboard.post.dto.PostRequest;
import com.fretboard.fretboard.post.service.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<Void> addPost(@Valid @RequestBody PostRequest postRequest) {
        Long postId = postService.addPost(postRequest);
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> findPost(@PathVariable Long id) {
        return ResponseEntity.ok().body(postService.findPost(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @Valid @RequestBody PostRequest postRequest) {
        postService.updatePost(id, postRequest);
        return ResponseEntity.noContent().build();
    }
}
