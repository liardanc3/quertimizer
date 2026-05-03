package com.quertimizer.problem.application.port.out;

public interface ProblemOutputPreviewRateLimitPort {

    void validate(String requester, String clientIp);
}
