package com.quertimizer.problem.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class ProblemRankingSubmitRecordOutput {

    private final String handle;
    private final DbmsType dbmsType;
    private final boolean success;

}
