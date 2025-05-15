package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.global.auth.annotation.LoginMember;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.post.dto.request.EditPostRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
import com.fretboard.fretboard.post.dto.request.NewPostRequest;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
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
    public ResponseEntity<Void> createPost(@Valid @RequestBody NewPostRequest request,
                                        @LoginMember MemberAuth memberAuth) {
        Long postId = postService.addPost(request, memberAuth);
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    @GetMapping(params = "boardId")
    public ResponseEntity<PostListResponse> getPostsByBoardId(@RequestParam Long boardId,
                                                               @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PostListResponse response = postService.findPostsByBoardId(boardId, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping(params = {"boardId", "keyword"})
    public ResponseEntity<PostListResponse> searchPosts(@RequestParam Long boardId,
                                                                 @RequestParam String keyword,
                                                                 @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PostListResponse response = postService.searchPosts(boardId, keyword, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok().body(postService.getPostDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePostDetails(@PathVariable Long id,
                                           @Valid @RequestBody EditPostRequest postRequest,
                                           @LoginMember MemberAuth memberAuth) {
        postService.updatePost(id, postRequest, memberAuth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removePost(@PathVariable Long id,
                                           @LoginMember MemberAuth memberAuth) {
        postService.deletePost(id, memberAuth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent-posts")
    public ResponseEntity<List<RecentPostsPerBoardResponse>> getRecentPostsPerBoard() {
        List<RecentPostsPerBoardResponse> response = postService.findRecentPostsPerBoard();
        return ResponseEntity.ok().body(response);
    }
}
