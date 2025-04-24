package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.global.auth.annotation.LoginMember;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.post.dto.PostDetailResponse;
import com.fretboard.fretboard.post.dto.PostSummaryResponse;
import com.fretboard.fretboard.post.dto.PostRequest;
import com.fretboard.fretboard.post.dto.PostListResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<Void> addPost(@Valid @RequestBody PostRequest request,
                                        @LoginMember MemberAuth memberAuth) {
        Long postId = postService.addPost(request);
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    @GetMapping(params = "boardId")
    public ResponseEntity<PostListResponse> findPostsByBoardId(@RequestParam Long boardId,
                                                               @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PostListResponse response = postService.findPostsByBoardId(boardId, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<List<PostSummaryResponse>> findPosts(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok().body(postService.findPosts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> findPost(@PathVariable Long id) {
        return ResponseEntity.ok().body(postService.findPost(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id,
                                           @Valid @RequestBody PostRequest postRequest,
                                           @LoginMember MemberAuth memberAuth) {
        postService.updatePost(id, postRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @LoginMember MemberAuth memberAuth) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
