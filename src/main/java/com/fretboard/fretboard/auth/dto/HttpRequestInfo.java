package com.fretboard.fretboard.auth.dto;

import org.springframework.http.HttpMethod;

public record HttpRequestInfo(HttpMethod method, String urlPattern) {

}
