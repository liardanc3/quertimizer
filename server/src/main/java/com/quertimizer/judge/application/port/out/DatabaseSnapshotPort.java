package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.model.DatabaseSnapshot;

public interface DatabaseSnapshotPort {

    DatabaseSnapshot createSnapshot();
}
