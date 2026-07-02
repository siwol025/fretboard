package com.fretboard.fretboard.image.controller;

import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.image.service.ImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ImageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ImageService imageService;

    @Test
    @DisplayName("비인증 요청 시 401 응답")
    void uploadImage_비인증_요청_401_응답() throws Exception {
        // given — memberId attribute 없이 요청 (비인증 상태)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        // when & then — memberId attribute 없으므로 LoginMemberArgumentResolver가 401 던져야 함
        mockMvc.perform(multipart("/api/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 요청 시 200 응답")
    void uploadImage_인증된_요청_200_응답() throws Exception {
        // given
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/images/test.jpg";
        given(imageService.upload(any())).willReturn(expectedUrl);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .requestAttr("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));
    }
}
