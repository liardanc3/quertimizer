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

    public static ProblemSetDetailRes from(ProblemSet problemSet) {
        return new ProblemSetDetailRes(
                problemSet.getProblemSetId(),
                normalize(problemSet.getDdlPostgresql()),
                normalize(problemSet.getDdlOracle()),
                normalize(problemSet.getDataPostgresql()),
                normalize(problemSet.getDataOracle())
        );
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }
}
