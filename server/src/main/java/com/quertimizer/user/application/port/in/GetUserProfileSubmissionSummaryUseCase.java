package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import java.util.Optional;

public interface GetUserProfileSubmissionSummaryUseCase {

    Optional<UserProfileSubmissionSummaryOutput> execute(UserProfileAccessInput input);
}
