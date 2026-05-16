package com.quertimizer.ranking.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RankingSnapshot {

    private final String snapshotId;
    private final DbmsType dbmsType;
    private final String handle;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final int solvedCountRank;
    private final int avgExecutionPercentileRank;
    private final int solvedCountRankDelta;
    private final int avgExecutionPercentileRankDelta;
    private final LocalDateTime calculatedAt;
}
