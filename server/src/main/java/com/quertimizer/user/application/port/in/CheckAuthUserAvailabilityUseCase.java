package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.AuthUserAvailabilityInput;
import com.quertimizer.user.application.output.AuthUserAvailabilityOutput;

public interface CheckAuthUserAvailabilityUseCase {

    AuthUserAvailabilityOutput execute(AuthUserAvailabilityInput input);
}
