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

    @NotBlank
    @Size(max = 20000)
    private String answerSql;

    @Size(max = 200000)
    private String sampleDataPostgresql;

    @Size(max = 200000)
    private String sampleDataMysql;

    @Size(max = 500000)
    private String actualDataPostgresql;

    @Size(max = 500000)
    private String actualDataMysql;

    @NotBlank
    private String problemSetMode;

    @NotBlank
    private String problemMode;

    private String problemSetId;

    private String problemId;

    private String dbms;

    @Size(max = 100000)
    private String ddlPostgresql;

    @Size(max = 100000)
    private String ddlMysql;

    public ProblemCreateInput toProblemCreateInput() {
        return new ProblemCreateInput(
                title,
                description,
                condition,
                output,
                answerSql,
                sampleDataPostgresql,
                sampleDataMysql,
                actualDataPostgresql,
                actualDataMysql,
                problemSetMode,
                problemMode,
                problemSetId,
                problemId,
                dbms,
                ddlPostgresql,
                ddlMysql
        );
    }
}
