package com.quertimizer.user.application.port.in;

import com.quertimizer.user.application.output.AuthUserOutput;

import java.util.List;

public interface GetAuthUsersUseCase {

    List<AuthUserOutput> execute();
}
