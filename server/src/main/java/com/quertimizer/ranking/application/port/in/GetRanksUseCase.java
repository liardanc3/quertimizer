package com.quertimizer.ranking.application.port.in;

import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankPageOutput;

public interface GetRanksUseCase {

    RankPageOutput execute(RankSearchInput input);
}
