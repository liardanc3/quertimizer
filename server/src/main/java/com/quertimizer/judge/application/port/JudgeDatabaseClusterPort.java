package com.quertimizer.judge.application.port;

import com.quertimizer.global.constant.DbmsType;

import java.util.List;

public interface JudgeDatabaseClusterPort {

    JudgeDatabaseLeasePort acquire(DbmsType engine);

    JudgeDatabaseLeasePort acquireNode(String nodeId);

    List<JudgeDatabaseNodePort> getConfiguredNodes();
}
