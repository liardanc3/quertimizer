package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedProblemsOutput {

    private final int solvedProblemCount;
    private final List<String> solvedProblemIds;
}
