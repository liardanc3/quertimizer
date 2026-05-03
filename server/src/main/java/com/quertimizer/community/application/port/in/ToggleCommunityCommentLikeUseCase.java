package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.ToggleCommunityCommentLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import java.util.Optional;

public interface ToggleCommunityCommentLikeUseCase {

    Optional<CommunityReactionOutput> execute(ToggleCommunityCommentLikeInput input);
}
