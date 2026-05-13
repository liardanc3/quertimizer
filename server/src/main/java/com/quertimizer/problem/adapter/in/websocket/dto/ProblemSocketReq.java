package com.quertimizer.problem.adapter.in.websocket.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemExecutionProgress;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import com.quertimizer.problem.adapter.in.http.support.ProblemSupport.WebSocketReplyTarget;
import lombok.Data;

import java.util.List;
import java.util.function.Consumer;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemSocketReq {

    private final String problemId;
    private final String sql;
    private final String dbms;
    private final Integer page;
    private final Integer pageSize;
    private final List<String> indexSqls;

    @JsonCreator
    public ProblemSocketReq(@JsonProperty("problemId") String problemId,
                            @JsonProperty("sql") String sql,
                            @JsonProperty("dbms") String dbms,
                            @JsonProperty("page") Integer page,
                            @JsonProperty("pageSize") Integer pageSize,
                            @JsonProperty("indexSqls") List<String> indexSqls) {
        this.problemId = problemId;
        this.sql = sql;
        this.dbms = dbms;
        this.page = page;
        this.pageSize = pageSize;
        this.indexSqls = indexSqls;
    }

    public String problemId() {
        return problemId;
    }

    public String sql() {
        return sql;
    }

    public String dbms() {
        return dbms;
    }

    public Integer page() {
        return page;
    }

    public Integer pageSize() {
        return pageSize;
    }

    public List<String> indexSqls() {
        return indexSqls;
    }

    public ProblemExecutionInput toInput(WebSocketReplyTarget replyTarget) {
        // WebSocket 요청과 응답 대상 기준 애플리케이션 입력 생성
        return toInput(replyTarget, progress -> {
        });
    }

    public ProblemExecutionInput toInput(WebSocketReplyTarget replyTarget,
                                         Consumer<ProblemExecutionProgress> progressListener) {
        // WebSocket 요청과 응답 대상 기준 애플리케이션 입력 생성
        return ProblemExecutionInput.of(
                replyTarget.getHandle(), replyTarget.getExecutionSessionId(),
                problemId, sql, dbms,
                page, pageSize, indexSqls,
                progressListener
        );
    }

    public ProblemSubmissionInput toSubmissionInput(WebSocketReplyTarget replyTarget,
                                                    Consumer<ProblemSubmissionProgress> progressListener) {
        // WebSocket 요청과 응답 대상 기준 제출 입력 생성
        return ProblemSubmissionInput.of(replyTarget.getHandle(), problemId, sql, dbms, indexSqls, progressListener);
    }
}
