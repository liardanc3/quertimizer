package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.ToggleCommunityPostLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import java.util.Optional;

public interface ToggleCommunityPostLikeUseCase {

    Optional<CommunityReactionOutput> execute(ToggleCommunityPostLikeInput input);
}
