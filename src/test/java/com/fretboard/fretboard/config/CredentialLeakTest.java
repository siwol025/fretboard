package com.fretboard.fretboard.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialLeakTest {

    // 민감 설정 키 목록 — 이 키들의 값은 반드시 ${ENV_VAR} 형식이어야 한다
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password:",
            "access-key:",
            "secret-key:"
    );

    // ${...} 환경변수 플레이스홀더 패턴
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{[^}]+}");

    @Test
    void application_prod_yml에_하드코딩된_자격증명이_없어야_한다() throws IOException {
        assertSensitiveKeysUseEnvVars("src/main/resources/application-prod.yml");
    }

    @Test
    void application_dev_yml에_하드코딩된_자격증명이_없어야_한다() throws IOException {
        assertSensitiveKeysUseEnvVars("src/main/resources/application-dev.yml");
    }

    @Test
    void application_test_yml에_고엔트로피_시크릿_없어야_한다() throws IOException {
        assertSensitiveKeysUseEnvVars("src/main/resources/application-test.yml", true);
    }

    private void assertSensitiveKeysUseEnvVars(String relativePath) throws IOException {
        assertSensitiveKeysUseEnvVars(relativePath, false);
    }

    /**
     * 민감 키(SENSITIVE_KEYS)의 값이 환경변수 플레이스홀더(${...})를 사용하는지 검증한다.
     * allowTestPrefix가 true이면 'test-'로 시작하는 값도 허용한다 (테스트 전용 설정 파일용).
     */
    private void assertSensitiveKeysUseEnvVars(String relativePath, boolean allowTestPrefix) throws IOException {
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            return; // gitignore된 파일이 없는 CI 환경에서는 통과
        }

        List<String> lines = Files.readAllLines(path);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String key : SENSITIVE_KEYS) {
                if (line.trim().startsWith(key)) {
                    String value = line.substring(line.indexOf(key) + key.length()).trim();
                    if (!value.isEmpty()) {
                        boolean isEnvVar = ENV_VAR_PATTERN.matcher(value).matches();
                        boolean isAccepted = isEnvVar || (allowTestPrefix && value.startsWith("test-"));
                        assertThat(isAccepted)
                                .as("'%s' %d번째 줄: '%s' 값이 환경변수 플레이스홀더(${...})%s를 사용해야 합니다. 현재 값: %s",
                                        relativePath, i + 1, key,
                                        allowTestPrefix ? " 또는 'test-' 접두사" : "",
                                        value)
                                .isTrue();
                    }
                }
            }
        }
    }
}
