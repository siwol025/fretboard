package com.fretboard.fretboard.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest(
        @NotBlank
        String reissueToken
) {
}
