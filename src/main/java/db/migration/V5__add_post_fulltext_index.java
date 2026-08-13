package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 게시글 제목·본문 전문검색 지원 — (title, content) 복합 FULLTEXT 인덱스 추가.
 *
 * <p>{@code PostRepository.searchByBoardIdAndKeyword} 가
 * {@code MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)} 를 실행하는데,
 * 이 인덱스가 없으면 errno 1191(Can't find FULLTEXT index matching the column list)로 실패한다.
 *
 * <p>{@code WITH PARSER ngram} 은 MySQL 전용 문법이라 SQL 마이그레이션으로 두면 테스트용
 * H2(MODE=MYSQL)에 Flyway 를 적용하는 경로에서 파싱 실패한다. 따라서 Java 마이그레이션으로 두고
 * 실제 DB 제품이 MySQL 일 때만 실행한다(H2 는 no-op → 전문검색은 MySQL 런타임/컨테이너에서만 검증).
 *
 * <p>한글 게시판이라 공백 경계 기반 기본 파서로는 CJK 토큰화가 되지 않으므로 ngram 파서 사용
 * (ngram_token_size 기본 2 → 2글자 단위 색인). MATCH 컬럼 순서/구성이 인덱스와 정확히 일치해야 한다.
 */
public class V5__add_post_fulltext_index extends BaseJavaMigration {

    @Override
    public void migrate(final Context context) throws Exception {
        final String product = context.getConnection().getMetaData().getDatabaseProductName();
        if (!product.toLowerCase().contains("mysql")) {
            return; // H2 등 비-MySQL 은 FULLTEXT ngram 미지원 → 건너뛴다.
        }
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(
                    "ALTER TABLE post "
                            + "ADD FULLTEXT INDEX ft_post_title_content (title, content) WITH PARSER ngram");
        }
    }
}
