package com.quertimizer.judge.infrastructure.execution;

import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class TemplateSchemaCopyProvisioningStrategy implements DatasetProvisioningStrategy {

    @Override
    public void provision(Connection connection, String schemaName, String ddl, String dataSql) {
        // TODO template-copy 전략은 template DB와 execution DB의 worker-local cache 동기화가 필요하다.
        throw new IllegalStateException("template-copy 전략은 아직 worker template cache 동기화가 구현되지 않았다. sql-replay를 사용해라.");
    }
}
