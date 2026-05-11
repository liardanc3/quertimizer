package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserAnomalyTrendItemOutput;
import lombok.Data;

@Data
public class UserAnomalyTrendItemRes {

    private final String handle;
    private final String actionType;
    private final long count;

    public static UserAnomalyTrendItemRes from(UserAnomalyTrendItemOutput result) {
        return new UserAnomalyTrendItemRes(
                result.getHandle(),
                result.getActionType(),
                result.getCount()
        );
    }
}
