package com.fretboard.fretboard.image.service;

import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.image.infrastructure.AwsS3Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageService {
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final AwsS3Provider awsS3Provider;

    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FretBoardException(ExceptionType.EMPTY_FILE);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new FretBoardException(ExceptionType.INVALID_FILE_TYPE);
        }
        return awsS3Provider.uploadImage(file);
    }

    public String convertTempImageUrlsToPermanent(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Elements images = doc.select("img");

        for (Element img : images) {
            String imgUrl = img.attr("src");
            String permanentUrl = awsS3Provider.copyImageToPermanentStorage(imgUrl);
            img.attr("src", permanentUrl);
        }

        return doc.body().html();
    }

    public void cleanUpRemovedImages(String oldHtml, String newHtml) {
        List<String> removedImageUrls = getRemovedImageUrls(oldHtml, newHtml);
        for (String url : removedImageUrls) {
            awsS3Provider.deleteImage(url);
        }
    }

    private List<String> getRemovedImageUrls(String before, String after) {
        List<String> beforeImages = new ArrayList<>(extract(before));
        List<String> afterImages = extract(after);
        beforeImages.removeAll(afterImages);
        return beforeImages;
    }

    private List<String> extract(String html) {
        Document doc = Jsoup.parse(html);
        return doc.select("img").stream().map(e -> e.attr("src")).toList();
    }

    public void deleteImage(String htmlContent) {
        List<String> removedImageUrls = extract(htmlContent);
        for (String url : removedImageUrls) {
            awsS3Provider.deleteImage(url);
        }
    }
}
