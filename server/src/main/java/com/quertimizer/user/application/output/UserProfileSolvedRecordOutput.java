package com.quertimizer.user.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileSolvedRecordOutput {

    private final String problemId;
    private final String problemTitle;
    private final String dbms;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;
}
