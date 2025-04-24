package com.fretboard.fretboard.auth.controller;

import com.fretboard.fretboard.auth.dto.request.LoginRequest;
import com.fretboard.fretboard.auth.dto.request.TokenReissueRequest;
import com.fretboard.fretboard.auth.dto.response.LoginResponse;
import com.fretboard.fretboard.auth.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/login")
public class LoginController {
    private final LoginService loginService;

    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok()
                .body(loginService.login(request));
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<LoginResponse> reissueToken(@Valid @RequestBody TokenReissueRequest request) {
        return ResponseEntity.ok(loginService.reissueToken(request));
    }
}
