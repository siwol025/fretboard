package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.service.PostFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/_bench")
public class BenchPostController {

    private final PostFeedService postFeedService;

    @GetMapping("/posts")
    public ResponseEntity<PostListResponse> getPostsByBoardIdOffset(
            @RequestParam Long boardId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PostListResponse response = postFeedService.getPostsByBoardIdOffset(boardId, pageable);
        return ResponseEntity.ok(response);
    }
}
