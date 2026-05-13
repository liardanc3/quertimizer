package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileSolvedProblemsRes {

    private final int solvedProblemCount;
    private final List<String> solvedProblemIds;

    public static UserProfileSolvedProblemsRes from(UserProfileSolvedProblemsOutput result) {
        return new UserProfileSolvedProblemsRes(result.getSolvedProblemCount(), result.getSolvedProblemIds());
    }
}
