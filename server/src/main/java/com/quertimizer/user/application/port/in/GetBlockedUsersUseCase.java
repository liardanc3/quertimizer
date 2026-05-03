package com.quertimizer.user.application.port.in;

import com.quertimizer.auth.application.output.BlockedUserPageOutput;
import com.quertimizer.user.application.input.BlockedAccountPageInput;

public interface GetBlockedUsersUseCase {

    BlockedUserPageOutput execute(BlockedAccountPageInput input);
}
