package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileUpdateCommandInput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import java.util.Optional;

public interface UpdateUserProfileUseCase {

    Optional<UserProfileSummaryOutput> execute(UserProfileUpdateCommandInput input);
}
