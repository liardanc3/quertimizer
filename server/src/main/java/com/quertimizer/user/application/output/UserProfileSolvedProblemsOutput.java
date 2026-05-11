package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileSolvedProblemsOutput {

    private final int solvedProblemCount;
    private final List<String> solvedProblemIds;
}
