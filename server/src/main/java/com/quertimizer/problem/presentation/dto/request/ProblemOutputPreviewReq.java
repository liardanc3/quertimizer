package com.quertimizer.problem.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProblemOutputPreviewReq {

    @NotBlank
    @Size(max = 20)
    private String dbms;

    @NotBlank
    @Size(max = 100000, message = "DDL은 최대 100000자까지 입력할 수 있습니다.")
    private String ddl;

    @NotBlank
    @Size(max = 200000, message = "예시 데이터 SQL은 최대 200000자까지 입력할 수 있습니다.")
    private String sampleDataSql;

    @NotBlank
    @Size(max = 20000, message = "정답 SQL은 최대 20000자까지 입력할 수 있습니다.")
    private String answerSql;
}
