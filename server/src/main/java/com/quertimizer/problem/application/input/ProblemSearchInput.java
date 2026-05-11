package com.quertimizer.problem.application.input;

import lombok.Data;

@Data
public class ProblemSearchInput {

    private final int page;
    private final String query;
    private final String dbms;
    private final String solveState;
    private final String currentHandle;
    private final String solvedCountSort;
    private final String totalSubmitSort;
    private final String successSubmitSort;
    private final String spreadRateSort;
    private final Double spreadRateMin;
    private final Double spreadRateMax;
}
