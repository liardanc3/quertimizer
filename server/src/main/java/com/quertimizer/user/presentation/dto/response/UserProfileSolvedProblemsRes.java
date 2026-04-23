package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedProblemsRes {

    private final int solvedProblemCount;
    private final List<String> solvedProblemIds;

}
