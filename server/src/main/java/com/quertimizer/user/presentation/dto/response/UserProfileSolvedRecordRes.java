package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileSolvedRecordRes {

    private final String problemId;
    private final String problemTitle;
    private final String dbms;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;

}
