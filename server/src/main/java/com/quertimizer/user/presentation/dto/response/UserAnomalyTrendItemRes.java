package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserAnomalyTrendItemOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
