package com.fretboard.fretboard.config;

import com.fretboard.fretboard.global.config.S3Config;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.lang.reflect.Method;
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

    @Test
    void boardInitializer_대용량_주석_블록_없음() throws IOException {
        Path path = Path.of("src/main/java/com/fretboard/fretboard/global/BoardInitializer.java");
        List<String> lines = Files.readAllLines(path);

        int consecutiveCommentCount = 0;
        int maxConsecutiveComments = 0;

        for (String line : lines) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("//")) {
                consecutiveCommentCount++;
                maxConsecutiveComments = Math.max(maxConsecutiveComments, consecutiveCommentCount);
            } else {
                consecutiveCommentCount = 0;
            }
        }

        assertThat(maxConsecutiveComments)
                .as("BoardInitializer.java에 연속 %d줄 주석 블록이 발견되었습니다. " +
                    "10줄 이상의 연속 주석 블록은 제거되어야 합니다.", maxConsecutiveComments)
                .isLessThan(10);
    }

    @Test
    void gitignore_application_local_yml_제외_설정_확인() throws IOException {
        Path gitignore = Path.of(".gitignore");
        assertThat(Files.exists(gitignore))
                .as(".gitignore 파일이 프로젝트 루트에 존재해야 합니다.")
                .isTrue();

        List<String> lines = Files.readAllLines(gitignore);
        boolean hasLocalYml = lines.stream()
                .map(String::trim)
                .anyMatch(line -> line.contains("application-local.yml"));

        assertThat(hasLocalYml)
                .as(".gitignore에 'application-local.yml' 패턴이 포함되어야 합니다. " +
                    "현재 application-local.yml이 .gitignore에서 누락된 상태입니다.")
                .isTrue();
    }

    @Test
    void s3Config_s3Client빈_destroyMethod_close_설정() throws Exception {
        Method method = S3Config.class.getMethod("s3Client");
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        assertThat(beanAnnotation).isNotNull();
        assertThat(beanAnnotation.destroyMethod())
                .as("S3Client 빈에 destroyMethod = \"close\" 가 설정되어야 합니다. " +
                    "미설정 시 애플리케이션 종료 시 네트워크 커넥션이 누수됩니다.")
                .isEqualTo("close");
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
