package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final double spreadRateMin;
    private final double spreadRateMax;
    private final List<ProblemListItemRes> problems;

}
