package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileSolvedRecordOutput {

    private final String problemId;
    private final String problemTitle;
    private final String dbms;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;
}
