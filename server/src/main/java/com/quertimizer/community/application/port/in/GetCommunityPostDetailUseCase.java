package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.CommunityPostDetailInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.output.CommunityPostDetailOutput;
import com.quertimizer.community.domain.entity.CommunityComment;
import java.util.Optional;

public interface GetCommunityPostDetailUseCase {

    Optional<CommunityPostDetailOutput> execute(CommunityPostDetailInput input);
}
