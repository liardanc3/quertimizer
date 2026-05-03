package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.output.CommunityImageOutput;
import java.util.Optional;

public interface GetCommunityImageUseCase {

    Optional<CommunityImageOutput> execute(String imageId);
}
