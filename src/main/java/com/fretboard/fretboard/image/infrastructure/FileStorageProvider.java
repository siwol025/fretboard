package com.fretboard.fretboard.image.infrastructure;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageProvider {

    String uploadImage(MultipartFile file);

    String copyImageToPermanentStorage(String imageUrl);

    void deleteImage(String imageUrl);
}
