package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.CreateCommunityPostInput;
import com.quertimizer.community.domain.entity.CommunityPost;

public interface CreateCommunityPostUseCase {

    Long execute(CreateCommunityPostInput input);
}
