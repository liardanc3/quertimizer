package com.quertimizer.problem.adapter.in.web.request;

import com.quertimizer.problem.application.input.ProblemSearchInput;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProblemSearchReq {

    @Min(0)
    @Max(1000)
    private int page = 1;

    @Size(max = 100)
    private String query;

    @Pattern(regexp = "postgresql|mysql|all")
    private String dbms = "postgresql";

    @Pattern(regexp = "all|solved|unsolved")
    private String solveState = "all";

    @Pattern(regexp = "none|asc|desc")
    private String solvedCountSort = "desc";

    @Pattern(regexp = "none|asc|desc")
    private String totalSubmitSort = "none";

    @Pattern(regexp = "none|asc|desc")
    private String successSubmitSort = "none";

    @Pattern(regexp = "none|asc|desc")
    private String spreadRateSort = "none";

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double spreadRateMin;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double spreadRateMax;

    public ProblemSearchInput toInput(String currentHandle) {
        return new ProblemSearchInput(
                page, query, dbms, solveState, currentHandle,
                solvedCountSort, totalSubmitSort, successSubmitSort, spreadRateSort,
                spreadRateMin, spreadRateMax
        );
    }
}
