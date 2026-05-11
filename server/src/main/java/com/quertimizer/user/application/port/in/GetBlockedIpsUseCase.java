package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.output.BlockedIpPageOutput;

public interface GetBlockedIpsUseCase {

    BlockedIpPageOutput execute(BlockedAccountPageInput input);
}
