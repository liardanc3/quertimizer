package com.quertimizer.auth.adapter.out.persistence;

import com.quertimizer.auth.application.port.out.VerificationCodeRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeInMemoryRepository implements VerificationCodeRepositoryPort {

    private final Map<String, String> emailToCodeMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> emailToExpiredAtMap = new ConcurrentHashMap<>();
    private final Set<String> verifiedEmailSet = ConcurrentHashMap.newKeySet();

    @Override
    public void saveCode(String email, String code, LocalDateTime expiredAt) {
        emailToCodeMap.put(email, code);
        emailToExpiredAtMap.put(email, expiredAt);
        verifiedEmailSet.remove(email);
    }

    @Override
    public Optional<String> findCode(String email) {
        return Optional.ofNullable(emailToCodeMap.get(email));
    }

    @Override
    public Optional<LocalDateTime> findExpiredAt(String email) {
        return Optional.ofNullable(emailToExpiredAtMap.get(email));
    }

    @Override
    public boolean isVerified(String email) {
        return verifiedEmailSet.contains(email);
    }

    @Override
    public void markVerified(String email) {
        verifiedEmailSet.add(email);
    }

    @Override
    public void clear(String email) {
        emailToCodeMap.remove(email);
        emailToExpiredAtMap.remove(email);
        verifiedEmailSet.remove(email);
    }

    @Override
    public void deleteExpired(LocalDateTime now) {
        emailToExpiredAtMap.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        emailToCodeMap.entrySet().removeIf(entry -> !emailToExpiredAtMap.containsKey(entry.getKey()));
        verifiedEmailSet.removeIf(email -> !emailToExpiredAtMap.containsKey(email));
    }
}
