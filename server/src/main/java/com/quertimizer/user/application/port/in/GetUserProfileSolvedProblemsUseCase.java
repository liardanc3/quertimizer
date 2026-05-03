package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import java.util.Optional;

public interface GetUserProfileSolvedProblemsUseCase {

    Optional<UserProfileSolvedProblemsOutput> execute(UserProfileAccessInput input);
}
