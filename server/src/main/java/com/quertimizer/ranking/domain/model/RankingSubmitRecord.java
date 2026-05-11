package com.quertimizer.ranking.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingSubmitRecord {

    private final String handle;
    private final DbmsType dbmsType;
    private final boolean success;

}
