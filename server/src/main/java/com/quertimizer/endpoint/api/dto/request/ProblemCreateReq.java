package com.quertimizer.endpoint.api.dto.request;

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

    @NotBlank
    private String problemSetMode;

    private String problemSetId;

    private String ddlPostgresql;

    private String ddlOracle;

    private String dataPostgresql;

    private String dataOracle;
}
