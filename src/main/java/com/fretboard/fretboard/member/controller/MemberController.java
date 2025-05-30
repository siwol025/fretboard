package com.fretboard.fretboard.member.controller;

import com.fretboard.fretboard.global.auth.annotation.LoginMember;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.member.dto.MemberEditRequest;
import com.fretboard.fretboard.member.dto.MemberEditResponse;
import com.fretboard.fretboard.member.dto.PasswordEditRequest;
import com.fretboard.fretboard.member.dto.SignupRequest;
import com.fretboard.fretboard.member.service.MemberService;
import com.fretboard.fretboard.post.dto.response.MyPostListResponse;
import com.fretboard.fretboard.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final PostService postService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest signupRequest) {
        memberService.signup(signupRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mypage/posts")
    public ResponseEntity<MyPostListResponse> getMyPosts(@LoginMember MemberAuth memberAuth,
                                                         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        MyPostListResponse response = postService.findMyPosts(memberAuth, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-changes")
    public ResponseEntity<Void> changePassword(@LoginMember MemberAuth memberAuth,
                                               @RequestBody PasswordEditRequest request) {
        memberService.updatePassword(memberAuth, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/edit-profile")
    public ResponseEntity<MemberEditResponse> editMemberInfo(@LoginMember MemberAuth memberAuth,
                                                             @RequestBody MemberEditRequest request) {
        MemberEditResponse memberEditResponse = memberService.updateMemberInfo(memberAuth, request);
        return ResponseEntity.ok().body(memberEditResponse);
    }
}
