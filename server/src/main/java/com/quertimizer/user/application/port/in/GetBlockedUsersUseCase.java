package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.output.BlockedUserPageOutput;

public interface GetBlockedUsersUseCase {

    BlockedUserPageOutput execute(BlockedAccountPageInput input);
}
