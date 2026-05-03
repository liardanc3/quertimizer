package com.quertimizer.ranking.application.port.in;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankListItemOutput;
import com.quertimizer.ranking.application.output.RankMonthlyDeltaOutput;
import com.quertimizer.ranking.application.output.RankPageOutput;
import com.quertimizer.ranking.domain.model.RankPageConstant;

public interface GetRanksUseCase {

    RankPageOutput execute(RankSearchInput input);
}
