package com.quertimizer.problem.application.port;

public interface ProblemOutputPreviewRateLimitPort {

    void validate(String requester, String clientIp);
}
