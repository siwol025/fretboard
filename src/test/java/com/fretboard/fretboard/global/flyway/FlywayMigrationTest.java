package com.fretboard.fretboard.global.flyway;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    @Test
    void V1_마이그레이션이_H2_테스트_DB에_적용됨() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        List<String> appliedVersions = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .toList();

        assertThat(appliedVersions)
                .hasSize(1)
                .containsExactly("1");
    }
}
