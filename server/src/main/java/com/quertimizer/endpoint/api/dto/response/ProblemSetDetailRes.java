package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.ProblemSet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSetDetailRes {

    private final String problemSetId;
    private final String ddlPostgresql;
    private final String ddlOracle;
    private final String dataPostgresql;
    private final String dataOracle;

    public static ProblemSetDetailRes from(String problemSetId, ProblemSet postgresqlProblemSet, ProblemSet oracleProblemSet) {
        return new ProblemSetDetailRes(
                problemSetId,
                postgresqlProblemSet != null ? normalize(postgresqlProblemSet.getDdl()) : "",
                oracleProblemSet != null ? normalize(oracleProblemSet.getDdl()) : "",
                postgresqlProblemSet != null ? normalize(postgresqlProblemSet.getData()) : "",
                oracleProblemSet != null ? normalize(oracleProblemSet.getData()) : ""
        );
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }
}
