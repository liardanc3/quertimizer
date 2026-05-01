package com.quertimizer.problem.domain.entity.ids;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProblemSolveHistoryId implements Serializable {

    private String problemId;
    private String handle;
}
