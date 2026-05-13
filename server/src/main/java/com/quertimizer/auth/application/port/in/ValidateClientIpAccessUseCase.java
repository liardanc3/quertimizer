package com.quertimizer.auth.application.port.in;

public interface ValidateClientIpAccessUseCase {

    void execute(String clientIp);
}
