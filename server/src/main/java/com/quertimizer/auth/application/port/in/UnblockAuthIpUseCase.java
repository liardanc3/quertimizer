package com.quertimizer.auth.application.port.in;

public interface UnblockAuthIpUseCase {

    void execute(String ipAddress);
}
