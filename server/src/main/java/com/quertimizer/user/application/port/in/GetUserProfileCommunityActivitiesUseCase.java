package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileActivityPageInput;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import java.util.Optional;

public interface GetUserProfileCommunityActivitiesUseCase {

    Optional<UserProfileCommunityActivitiesOutput> execute(UserProfileActivityPageInput input);
}
