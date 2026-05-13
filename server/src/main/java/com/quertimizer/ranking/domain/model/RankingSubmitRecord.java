package com.quertimizer.ranking.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class RankingSubmitRecord {

    private final String handle;
    private final DbmsType dbmsType;
    private final boolean success;

}
