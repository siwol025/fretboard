package com.fretboard.fretboard.image.service;

import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.image.infrastructure.FileStorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private FileStorageProvider fileStorageProvider;

    @InjectMocks
    private ImageService imageService;

    @Test
    void upload_허용되지않은_MIME_타입이면_INVALID_FILE_TYPE_예외() {
        // given
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("application/pdf");

        // when & then
        assertThatThrownBy(() -> imageService.upload(file))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fretBoardException = (FretBoardException) ex;
                    assertThat(fretBoardException.getExceptionType()).isEqualTo(ExceptionType.INVALID_FILE_TYPE);
                });
    }

    @Test
    void upload_빈파일이면_EMPTY_FILE_예외() {
        // given
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(true);

        // when & then
        assertThatThrownBy(() -> imageService.upload(file))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fretBoardException = (FretBoardException) ex;
                    assertThat(fretBoardException.getExceptionType()).isEqualTo(ExceptionType.EMPTY_FILE);
                });
    }

    @Test
    void upload_허용된_MIME_타입이면_성공() {
        // given
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getContentType()).willReturn("image/jpeg");
        given(fileStorageProvider.uploadImage(file)).willReturn("https://s3.amazonaws.com/bucket/image.jpg");

        // when
        String result = imageService.upload(file);

        // then
        assertThat(result).isEqualTo("https://s3.amazonaws.com/bucket/image.jpg");
    }
}
