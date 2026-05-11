package com.quertimizer.problem.adapter.in.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateProgress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

@Data
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

    @Size(max = 100000)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private String problemDdl = "";

    @Size(max = 10)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@Size(max = 500000) String> hiddenDataSqls = List.of();

    public ProblemCreateInput toInput() {
        return toInput(null);
    }

    public ProblemCreateInput toInput(Consumer<ProblemCreateProgress> progressListener) {
        return new ProblemCreateInput(
                title, description, condition, output, answerSql,
                actualDataSql, problemSetId, problemId, dbms, ddl, problemDdl,
                hiddenDataSqls, progressListener
        );
    }
}
