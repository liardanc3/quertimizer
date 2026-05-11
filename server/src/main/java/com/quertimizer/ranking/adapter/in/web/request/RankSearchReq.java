package com.quertimizer.ranking.adapter.in.web.request;

import com.quertimizer.ranking.application.input.RankSearchInput;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RankSearchReq {

    @Min(0)
    @Max(1000)
    private int page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize;

    @Pattern(regexp = "postgresql|mysql")
    private String dbms = "postgresql";

    @Size(max = 100)
    private String query;

    @Pattern(regexp = "solvedCount|avgExecutionPercentile|totalSubmitCount|successSubmitCount")
    private String sortKey = "solvedCount";

    public RankSearchInput toInput() {
        return new RankSearchInput(page, pageSize, dbms, query, sortKey);
    }
}
