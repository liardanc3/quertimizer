package com.quertimizer.entity;

import java.io.Serializable;

public record ProblemSolveHistoryId(String problemId, String userId) implements Serializable {
}
