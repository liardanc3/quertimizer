package com.quertimizer.ranking.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class RankingSnapshotJpaId implements Serializable {

    private String snapshotId;
    private DbmsType dbmsType;
    private String handle;

    public RankingSnapshotJpaId(String snapshotId, DbmsType dbmsType, String handle) {
        this.snapshotId = snapshotId;
        this.dbmsType = dbmsType;
        this.handle = handle;
    }
}
