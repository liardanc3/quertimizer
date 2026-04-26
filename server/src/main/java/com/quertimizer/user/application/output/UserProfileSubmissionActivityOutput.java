package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileSubmissionActivityOutput {

    private final String date;
    private final long count;
}
