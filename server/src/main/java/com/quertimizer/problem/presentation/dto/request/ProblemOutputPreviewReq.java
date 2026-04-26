package com.quertimizer.problem.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProblemOutputPreviewReq {

    @NotBlank
    private String dbms;

    @NotBlank
    private String ddl;

    @NotBlank
    private String sampleDataSql;

    @NotBlank
    private String answerSql;
}
