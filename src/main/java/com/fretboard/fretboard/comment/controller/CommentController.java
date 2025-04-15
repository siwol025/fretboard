package com.fretboard.fretboard.comment.controller;

import com.fretboard.fretboard.comment.dto.CommentRequest;
import com.fretboard.fretboard.comment.dto.CommentResponse;
import com.fretboard.fretboard.comment.service.CommentService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Void> addComment(@PathVariable Long postId,
                                           @Valid @RequestBody CommentRequest commentRequest) {
        Long commentId = commentService.addComment(postId, commentRequest);
        return ResponseEntity.created(URI.create("/comments/" + commentId)).build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> findComments(@PathVariable Long postId) {
        CommentResponse comments = commentService.findComments(postId);
        return ResponseEntity.ok().body(comments);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<Void> editComment(@PathVariable Long id,
                                            @Valid @RequestBody CommentRequest commentRequest) {
        commentService.editComment(id, commentRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
