package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.CommunityPostSearchInput;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.model.CommunityPostConstant;

public interface GetCommunityPostsUseCase {

    CommunityPostPageOutput execute(CommunityPostSearchInput input);
}
