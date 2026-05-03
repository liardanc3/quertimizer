package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import java.util.Optional;

public interface GetUserProfileLikedPostsUseCase {

    Optional<UserProfileCommunityPostsOutput> execute(UserProfileAccessInput input);
}
