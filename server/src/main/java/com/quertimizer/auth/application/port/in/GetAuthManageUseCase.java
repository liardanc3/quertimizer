package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.output.AuthManageOutput;
import com.quertimizer.auth.application.output.AuthManageUserRowOutput;

public interface GetAuthManageUseCase {

    AuthManageOutput execute();
}
