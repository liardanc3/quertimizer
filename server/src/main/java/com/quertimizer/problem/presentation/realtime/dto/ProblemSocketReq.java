package com.quertimizer.problem.presentation.realtime.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import com.quertimizer.problem.presentation.support.ProblemSupport.StompReplyTarget;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemSocketReq {

    private final String problemId;
    private final String sql;
    private final String dbms;
    private final Integer page;
    private final Integer pageSize;

    @JsonCreator
    public ProblemSocketReq(@JsonProperty("problemId") String problemId,
                            @JsonProperty("sql") String sql,
                            @JsonProperty("dbms") String dbms,
                            @JsonProperty("page") Integer page,
                            @JsonProperty("pageSize") Integer pageSize) {
        this.problemId = problemId;
        this.sql = sql;
        this.dbms = dbms;
        this.page = page;
        this.pageSize = pageSize;
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

    public ProblemExecutionInput toInput(StompReplyTarget replyTarget) {
        // STOMP 요청과 응답 대상 기준 애플리케이션 입력 생성
        return ProblemExecutionInput.of(
                replyTarget.getHandle(), replyTarget.getExecutionSessionId(),
                problemId, sql, dbms,
                page, pageSize
        );
    }

    public ProblemSubmissionInput toSubmissionInput(StompReplyTarget replyTarget,
                                                    Consumer<ProblemSubmissionProgress> progressListener) {
        // STOMP 요청과 응답 대상 기준 제출 입력 생성
        return ProblemSubmissionInput.of(replyTarget.getHandle(), problemId, sql, dbms, progressListener);
    }
}
