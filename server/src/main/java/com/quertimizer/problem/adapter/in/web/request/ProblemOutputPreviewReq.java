package com.quertimizer.problem.adapter.in.web.request;

import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProblemOutputPreviewReq {

    @NotBlank
    @Pattern(regexp = "postgresql|mysql", message = "DBMS는 postgresql 또는 mysql만 사용할 수 있습니다.")
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

    public ProblemOutputPreviewInput toInput(String requester, String clientIp) {
        return new ProblemOutputPreviewInput(
                dbms, ddl, sampleDataSql,
                answerSql, requester, clientIp
        );
    }
}
