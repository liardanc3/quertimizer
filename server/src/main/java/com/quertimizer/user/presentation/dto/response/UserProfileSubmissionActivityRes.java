package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileSubmissionActivityOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileSubmissionActivityRes {

    private final String date;
    private final long count;

    public static UserProfileSubmissionActivityRes from(UserProfileSubmissionActivityOutput result) {
        return new UserProfileSubmissionActivityRes(result.getDate(), result.getCount());
    }
}
