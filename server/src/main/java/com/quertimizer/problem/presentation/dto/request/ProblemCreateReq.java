package com.quertimizer.problem.presentation.dto.request;

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
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String condition;

    @NotBlank
    private String output;

    private String outputSample;

    private String answer;

    private String answerSql;

    @NotBlank
    private String problemSetMode;

    @NotBlank
    private String problemMode;

    private String problemSetId;

    private String problemId;

    private String dbms;

    private String ddlPostgresql;

    private String ddlOracle;

    private String dataPostgresql;

    private String dataOracle;

    public ProblemCreateInput toProblemCreateInput() {
        return new ProblemCreateInput(
                title,
                description,
                condition,
                output,
                outputSample,
                answer,
                answerSql,
                problemSetMode,
                problemMode,
                problemSetId,
                problemId,
                dbms,
                ddlPostgresql,
                ddlOracle,
                dataPostgresql,
                dataOracle
        );
    }
}
