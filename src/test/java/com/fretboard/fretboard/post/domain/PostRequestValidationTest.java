package com.fretboard.fretboard.post.domain;

import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PostRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void PostNewRequest_content_10001자_초과_시_검증_실패() {
        PostNewRequest request = new PostNewRequest(1L, "제목", "가".repeat(10001));
        Set<ConstraintViolation<PostNewRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void PostNewRequest_content_10000자_이하_검증_성공() {
        PostNewRequest request = new PostNewRequest(1L, "제목", "가".repeat(10000));
        Set<ConstraintViolation<PostNewRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void PostEditRequest_content_10001자_초과_시_검증_실패() {
        PostEditRequest request = new PostEditRequest("제목", "가".repeat(10001));
        Set<ConstraintViolation<PostEditRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void PostEditRequest_content_10000자_이하_검증_성공() {
        PostEditRequest request = new PostEditRequest("제목", "가".repeat(10000));
        Set<ConstraintViolation<PostEditRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
