package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserAnomalyTrendSearchInput;
import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;

public interface GetUserAnomalyTrendsUseCase {

    UserAnomalyTrendPageOutput execute(UserAnomalyTrendSearchInput input);
}
