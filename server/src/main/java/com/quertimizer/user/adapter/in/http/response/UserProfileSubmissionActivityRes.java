package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileSubmissionActivityOutput;
import lombok.Data;

@Data
public class UserProfileSubmissionActivityRes {

    private final String date;
    private final long count;

    public static UserProfileSubmissionActivityRes from(UserProfileSubmissionActivityOutput result) {
        return new UserProfileSubmissionActivityRes(result.getDate(), result.getCount());
    }
}
