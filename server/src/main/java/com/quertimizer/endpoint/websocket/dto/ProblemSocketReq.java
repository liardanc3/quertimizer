package com.quertimizer.endpoint.websocket.dto;

public record ProblemSocketReq(String type,
                               String problemId,
                               String sql,
                               String dbms,
                               Integer page,
                               Integer pageSize) {
}
