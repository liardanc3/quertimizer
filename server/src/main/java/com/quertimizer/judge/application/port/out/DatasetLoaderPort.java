package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.domain.entity.DatasetDefinition;

public interface DatasetLoaderPort {

    void waitUntilReady(Database templateDatabase, int startupTimeoutSeconds);

    void load(Database templateDatabase, String environmentName, DatasetDefinition dataset) throws Exception;
}
