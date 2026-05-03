package com.quertimizer.problem.adapter.in.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProblemCreateReq {

    @NotBlank
    @Size(max = 200)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String title = "";

    @NotBlank
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String description = "";

    @NotBlank
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String condition = "";

    @NotBlank
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String output = "";

    @NotBlank(message = "정답 기준 SQL이 필요합니다.")
    @Size(max = 20000)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String answerSql = "";

    @NotBlank(message = "예시 데이터 SQL이 필요합니다.")
    @Size(max = 200000)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String sampleDataSql = "";

    @NotBlank(message = "실제 채점 데이터 SQL이 필요합니다.")
    @Size(max = 500000)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String actualDataSql = "";

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String problemSetId = "";

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String problemId = "";

    @NotBlank(message = "DBMS 정보가 필요합니다.")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String dbms = "";

    @NotBlank(message = "DDL이 필요합니다.")
    @Size(max = 100000)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String ddl = "";

    public ProblemCreateInput toInput() {
        return new ProblemCreateInput(
                title, description, condition, output, answerSql,
                sampleDataSql, actualDataSql,
                problemSetId, problemId, dbms, ddl
        );
    }
}
