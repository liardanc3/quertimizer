package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import java.util.Optional;

public interface GetUserProfileSolvedRecordsUseCase {

    Optional<UserProfileSolvedRecordsOutput> execute(UserProfileAccessInput input);
}
