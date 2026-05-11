package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemRankingRecordsOutput {

    private final List<ProblemRankingSolveRecordOutput> solveRecords;
    private final List<ProblemRankingSubmitRecordOutput> submitRecords;

}
