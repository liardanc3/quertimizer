package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedProblemsRes {

    private final int solvedProblemCount;
    private final List<String> solvedProblemIds;

    public static UserProfileSolvedProblemsRes from(UserProfileSolvedProblemsOutput result) {
        return new UserProfileSolvedProblemsRes(result.getSolvedProblemCount(), result.getSolvedProblemIds());
    }
}
