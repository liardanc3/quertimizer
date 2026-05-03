package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.DeleteCommunityPostInput;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;

public interface DeleteCommunityPostUseCase {

    boolean execute(DeleteCommunityPostInput input);
}
