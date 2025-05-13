package com.fretboard.fretboard.comment.controller;

import com.fretboard.fretboard.comment.dto.CommentRequest;
import com.fretboard.fretboard.comment.dto.CommentResponse;
import com.fretboard.fretboard.comment.service.CommentService;
import com.fretboard.fretboard.global.auth.annotation.LoginMember;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Void> addComment(@PathVariable Long postId,
                                           @LoginMember MemberAuth memberAuth,
                                           @Valid @RequestBody CommentRequest request) {
        Long commentId = commentService.addComment(postId, request, memberAuth);
        return ResponseEntity.created(URI.create("/comments/" + commentId)).build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> findComments(@PathVariable Long postId) {
        CommentResponse comments = commentService.findComments(postId);
        return ResponseEntity.ok().body(comments);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<Void> editComment(@PathVariable Long id,
                                            @LoginMember MemberAuth memberAuth,
                                            @Valid @RequestBody CommentRequest request) {
        commentService.editComment(id, request, memberAuth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @LoginMember MemberAuth memberAuth) {
        commentService.deleteComment(id, memberAuth);
        return ResponseEntity.noContent().build();
    }
}
