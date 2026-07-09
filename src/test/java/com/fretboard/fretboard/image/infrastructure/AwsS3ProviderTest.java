package com.fretboard.fretboard.image.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsS3ProviderTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile mockFile;

    private AwsS3Provider awsS3Provider;

    @BeforeEach
    void setUp() {
        awsS3Provider = new AwsS3Provider(
                s3Client,
                "test-bucket",
                "https://cdn.example.com/",
                "temp/",
                "images/"
        );
    }

    @Test
    @DisplayName("uploadImage 호출 시 getInputStream()을 사용하고 getBytes()를 호출하지 않는다")
    void uploadImage_usesInputStream_notGetBytes() throws IOException {
        given(mockFile.getOriginalFilename()).willReturn("test.jpg");
        given(mockFile.getContentType()).willReturn("image/jpeg");
        given(mockFile.getSize()).willReturn(4L);
        given(mockFile.getInputStream()).willReturn(new ByteArrayInputStream("test".getBytes()));

        awsS3Provider.uploadImage(mockFile);

        verify(mockFile).getInputStream();
        verify(mockFile, never()).getBytes();
    }
}
