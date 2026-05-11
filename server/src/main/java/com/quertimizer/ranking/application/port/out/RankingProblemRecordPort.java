package com.quertimizer.ranking.application.port.out;

import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;

import java.util.List;

public interface RankingProblemRecordPort {

    List<RankingSolveRecord> findSolveRecords();

    List<RankingSubmitRecord> findSubmitRecords();

}
