package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import java.util.Optional;

public interface GetUserProfileSummaryUseCase {

    Optional<UserProfileSummaryOutput> execute(UserProfileAccessInput input);
}
