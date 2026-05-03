package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import java.util.Optional;

public interface GetUserProfileLikedCommentsUseCase {

    Optional<UserProfileCommunityCommentsOutput> execute(UserProfileAccessInput input);
}
