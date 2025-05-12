package com.fretboard.fretboard.image.service;

import com.fretboard.fretboard.image.infrastructure.AwsS3Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final AwsS3Provider awsS3Provider;

    public String upload(MultipartFile file) {
        return awsS3Provider.uploadImage(file);
    }
}
