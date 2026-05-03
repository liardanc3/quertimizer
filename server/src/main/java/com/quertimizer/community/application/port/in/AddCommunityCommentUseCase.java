package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.AddCommunityCommentInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.domain.entity.CommunityComment;
import java.util.Optional;

public interface AddCommunityCommentUseCase {

    Optional<CommunityCommentOutput> execute(AddCommunityCommentInput input);
}
