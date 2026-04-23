package com.quertimizer.problem.domain.entity;

import java.io.Serializable;

public record ProblemSolveHistoryId(String problemId, String handle) implements Serializable {
}
