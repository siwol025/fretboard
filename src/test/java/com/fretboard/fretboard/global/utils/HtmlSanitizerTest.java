package com.fretboard.fretboard.global.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void script태그_제거됨() {
        String input = "<script>alert('xss')</script>악성코드";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).doesNotContain("<script>").doesNotContain("alert(");
    }

    @Test
    void onerror_속성_제거됨() {
        String input = "<img src=\"x\" onerror=\"alert(1)\">";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).doesNotContain("onerror");
    }

    @Test
    void javascript_href_제거됨() {
        String input = "<a href=\"javascript:alert(1)\">클릭</a>";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    void 정상_텍스트_유지됨() {
        String input = "안녕하세요 <b>반갑습니다</b>";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).contains("안녕하세요").contains("<b>반갑습니다</b>");
    }

    @Test
    void 정상_img태그_유지됨() {
        String input = "<img src=\"https://example.com/img.png\" alt=\"이미지\">";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).contains("img").contains("example.com/img.png");
    }
}
