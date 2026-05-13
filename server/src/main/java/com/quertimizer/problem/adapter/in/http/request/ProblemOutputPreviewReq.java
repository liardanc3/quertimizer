package com.quertimizer.problem.adapter.in.http.request;

import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
    @Size(max = 500000, message = "실제 채점 데이터 SQL은 최대 500000자까지 입력할 수 있습니다.")
    private String actualDataSql;

    @NotBlank
    @Size(max = 20000, message = "정답 SQL은 최대 20000자까지 입력할 수 있습니다.")
    private String answerSql;

    public ProblemOutputPreviewInput toInput(String requester, String clientIp) {
        return new ProblemOutputPreviewInput(
                dbms, ddl, actualDataSql,
                answerSql, requester, clientIp
        );
    }
}
