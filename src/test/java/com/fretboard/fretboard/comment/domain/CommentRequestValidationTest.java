package com.fretboard.fretboard.comment.domain;

import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void content_1001자_초과_시_검증_실패() {
        CommentRequest request = new CommentRequest("가".repeat(1001));
        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void content_1000자_이하_검증_성공() {
        CommentRequest request = new CommentRequest("가".repeat(1000));
        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void content_비어있을때_메시지_확인() {
        CommentRequest request = new CommentRequest("");
        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("댓글을 입력해주세요.");
    }
}
