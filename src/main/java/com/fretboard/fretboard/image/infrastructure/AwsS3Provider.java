package com.fretboard.fretboard.image.infrastructure;

import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class AwsS3Provider {
    private final S3Client s3Client;
    private final String bucket;
    private final String imageBaseUri;
    private final String temporaryStoragePath;
    private final String imageStoragePath;

    public AwsS3Provider(
            S3Client s3Client,
            @Value("${cloud.aws.s3.bucket}") String bucket,
            @Value("${cloud.aws.s3.image-base-uri}") String imageBaseUri,
            @Value("${cloud.aws.s3.temporary-storage-path}") String temporaryStoragePath,
            @Value("${cloud.aws.s3.image-storage-path}") String imageStoragePath
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.imageBaseUri = imageBaseUri;
        this.temporaryStoragePath = temporaryStoragePath;
        this.imageStoragePath = imageStoragePath;
    }

    public String uploadImage(MultipartFile file) {
        String newFileName = createNewFileName(file.getOriginalFilename());
        String filePath = temporaryStoragePath + newFileName;
        uploadFile(file, filePath);
        return imageBaseUri + filePath;
    }

    private String createNewFileName(String fileName) {
        return UUID.randomUUID() + fileName.substring(fileName.lastIndexOf("."));
    }

    private void uploadFile(MultipartFile file, String filePath) {
        try {
            RequestBody requestBody = RequestBody.fromBytes(file.getBytes());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, requestBody);
        } catch (IOException e) {
            throw new FretBoardException(ExceptionType.S3_UPLOAD_EXCEPTION);
        }
    }

    public String copyImageToPermanentStorage(String imageUrl) {
        if (isInPermanentStorage(imageUrl)) {
            return imageUrl;
        }

        validateS3Path(imageUrl);
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String sourceKey = temporaryStoragePath + fileName;
        String destinationKey = imageStoragePath + fileName;
        copyFile(sourceKey, destinationKey);
        return imageUrl.replace(temporaryStoragePath, imageStoragePath);
    }

    private boolean isInPermanentStorage(String imageUrl) {
        return imageUrl.startsWith(imageBaseUri + imageStoragePath);
    }

    private void validateS3Path(String imageUrl) {
        if (!imageUrl.startsWith(imageBaseUri + temporaryStoragePath)) {
            throw new FretBoardException(ExceptionType.S3_FORMAT_EXCEPTION);
        }
    }

    private void copyFile(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(request);
        } catch (NoSuchKeyException exception) {
            throw new FretBoardException(ExceptionType.S3_NOT_FOUND);
        }
    }

    public void deleteImage(String imageUrl) {
        validateDeletablePath(imageUrl);
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String filePath = extractStoragePath(imageUrl) + fileName;

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new FretBoardException(ExceptionType.S3_DELETE_EXCEPTION);
        }
    }

    private void validateDeletablePath(String imageUrl) {
        boolean isTemporary = imageUrl.startsWith(imageBaseUri + temporaryStoragePath);
        boolean isPermanent = imageUrl.startsWith(imageBaseUri + imageStoragePath);
        if (!isTemporary && !isPermanent) {
            throw new FretBoardException(ExceptionType.S3_FORMAT_EXCEPTION);
        }
    }

    private String extractStoragePath(String imageUrl) {
        if (imageUrl.startsWith(imageBaseUri + temporaryStoragePath)) {
            return temporaryStoragePath;
        }
        return imageStoragePath;
    }
}
