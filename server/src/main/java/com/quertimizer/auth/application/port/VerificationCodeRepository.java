package com.quertimizer.auth.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository {

    void saveCode(String email, String code, LocalDateTime expiredAt);

    Optional<String> findCode(String email);

    Optional<LocalDateTime> findExpiredAt(String email);

    boolean isVerified(String email);

    void markVerified(String email);

    void clear(String email);

    void deleteExpired(LocalDateTime now);
}
