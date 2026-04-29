package com.quertimizer.problem.infrastructure.sqljudge;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

/**
 * 문제 인프라 어댑터가 sql-judge 런타임 DB 설정을 바인딩하기 위한 설정 클래스다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "judge")
public class ProblemSqlJudgeProperties {

    private List<DatabaseProperties> databases = List.of();

    /**
     * 설정된 런타임 DB 목록을 반환한다.
     *
     * @return 설정된 런타임 DB 목록
     */
    public List<DatabaseProperties> getDatabases() {
        // 설정 누락 시에도 sql-judge 빈 생성 단계에서 null 참조가 나지 않도록 빈 목록으로 정리한다.
        return databases != null ? databases : List.of();
    }

    /**
     * sql-judge 런타임 DB 하나에 대한 설정이다.
     */
    @Getter
    @Setter
    public static class DatabaseProperties {
        private String id;
        private String name;
        private String engine;
        private String dbmsType;
        private String url;
        private String username;
        private String password;
        private boolean enabled = true;
        private int maxConcurrency = 1;
        private Integer weight;

        /**
         * 설정된 DBMS 유형을 해석한다.
         *
         * @return 유효한 DBMS 유형
         */
        public Optional<DbmsType> resolveEngine() {
            // 기존 설정 키와 새 설정 키를 모두 허용해 로컬/운영 설정 전환 비용을 줄인다.
            return DbmsType.fromValue(engine != null && !engine.isBlank() ? engine : dbmsType);
        }
    }
}
