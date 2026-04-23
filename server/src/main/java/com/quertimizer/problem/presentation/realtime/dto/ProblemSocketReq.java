package com.quertimizer.problem.presentation.realtime.dto;

public record ProblemSocketReq(String type,
                               String problemId,
                               String sql,
                               String dbms,
                               Integer page,
                               Integer pageSize) {
}
