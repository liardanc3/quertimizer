package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.input.CommunityImageUploadInput;
import com.quertimizer.community.application.output.CommunityImageOutput;

public interface UploadCommunityImageUseCase {

    CommunityImageOutput execute(CommunityImageUploadInput input);
}
