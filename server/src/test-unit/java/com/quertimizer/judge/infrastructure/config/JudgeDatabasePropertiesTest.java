package com.quertimizer.judge.infrastructure.config;

import com.quertimizer.global.constant.DbmsType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeDatabasePropertiesTest {

    @Test
    @DisplayName("judge.databases 리스트 설정을 바인딩한다")
    void bindJudgeDatabaseNodeList() {
        // given
        MockEnvironment environment = new MockEnvironment()
                .withProperty("judge.dataset-provisioning.strategy", "sql-replay")
                .withProperty("judge.databases[0].id", "pg-worker-1")
                .withProperty("judge.databases[0].name", "pg-worker-1")
                .withProperty("judge.databases[0].engine", "postgresql")
                .withProperty("judge.databases[0].url", "jdbc:postgresql://localhost:5432/postgres")
                .withProperty("judge.databases[0].username", "postgres")
                .withProperty("judge.databases[0].password", "test-password")
                .withProperty("judge.databases[0].enabled", "true")
                .withProperty("judge.databases[0].max-concurrency", "2")
                .withProperty("judge.databases[1].id", "mysql-worker-1")
                .withProperty("judge.databases[1].engine", "mysql")
                .withProperty("judge.databases[1].url", "jdbc:mysql://localhost:3306/quertimizer")
                .withProperty("judge.databases[1].username", "mysql")
                .withProperty("judge.databases[1].enabled", "false");

        // when
        JudgeDatabaseProperties properties = Binder.get(environment)
                .bind("judge", Bindable.of(JudgeDatabaseProperties.class))
                .orElseThrow();

        // then
        assertEquals("sql-replay", properties.getProvisioningStrategy());
        assertEquals(2, properties.getDatabases().size());
        assertEquals(1, properties.getDatabases(DbmsType.POSTGRESQL).size());
        assertEquals(1, properties.getDatabases(DbmsType.MYSQL).size());
        assertEquals(2, properties.getDatabases(DbmsType.POSTGRESQL).get(0).getMaxConcurrency());
        assertTrue(properties.getDatabases(DbmsType.MYSQL).get(0).resolveEngine().filter(DbmsType.MYSQL::equals).isPresent());
    }
}
