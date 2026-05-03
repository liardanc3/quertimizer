package com.quertimizer.user.application.port.in;

import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import com.quertimizer.user.application.input.BlockedAccountPageInput;

public interface GetBlockedIpsUseCase {

    BlockedIpPageOutput execute(BlockedAccountPageInput input);
}
