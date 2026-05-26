package com.quertimizer.problem.application.input;

import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

@Data
public class ProblemUserSubmitCountInput {

    private final LocalDateTime submittedStart;
    private final LocalDateTime submittedEnd;
    private final Pageable pageable;
}
