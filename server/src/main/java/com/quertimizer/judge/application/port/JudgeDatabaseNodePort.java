package com.quertimizer.judge.application.port;

import com.quertimizer.global.constant.DbmsType;

public interface JudgeDatabaseNodePort {

    String getId();

    DbmsType getEngine();

    boolean isReady();
}
