package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.UpdateCommunityPostInput;
import java.util.Optional;

public interface UpdateCommunityPostUseCase {

    Optional<Long> execute(UpdateCommunityPostInput input);
}
