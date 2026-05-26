package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.quertimizer.judge.application.model.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MySqlDialect")
class MySqlDialectTest {

    private final MySqlDialect dialect = new MySqlDialect();

    @Nested
    @DisplayName("persistentStatisticsSqls")
    class PersistentStatisticsSqls {

        @Test
        @DisplayName("성공 (테이블별 영구 통계 옵션 생성)")
        void successWhenTableNamesExist() {
            // given
            List<String> tableNames = List.of("customers", "odd`table");

            // when
            List<String> sqls = dialect.persistentStatisticsSqls(tableNames);

            // then
            assertThat(sqls).containsExactly(
                    "ALTER TABLE `customers` STATS_PERSISTENT = 1, STATS_AUTO_RECALC = 0, STATS_SAMPLE_PAGES = %d"
                            .formatted(Constants.MYSQL_INNODB_STATS_PERSISTENT_SAMPLE_PAGES),
                    "ALTER TABLE `odd``table` STATS_PERSISTENT = 1, STATS_AUTO_RECALC = 0, STATS_SAMPLE_PAGES = %d"
                            .formatted(Constants.MYSQL_INNODB_STATS_PERSISTENT_SAMPLE_PAGES)
            );
        }
    }

    @Nested
    @DisplayName("analyzeTablesSql")
    class AnalyzeTablesSql {

        @Test
        @DisplayName("성공 (테이블 목록 기준 ANALYZE TABLE 생성)")
        void successWhenTableNamesExist() {
            // given
            List<String> tableNames = List.of("customers", "customer_orders", "odd`table");

            // when
            String sql = dialect.analyzeTablesSql(tableNames);

            // then
            assertThat(sql).isEqualTo("ANALYZE TABLE `customers`, `customer_orders`, `odd``table`");
        }

        @Test
        @DisplayName("성공 (테이블 없음)")
        void successWhenTableNamesAreEmpty() {
            // given
            List<String> tableNames = List.of();

            // when
            String sql = dialect.analyzeTablesSql(tableNames);

            // then
            assertThat(sql).isEmpty();
        }
    }
}
