package com.quertimizer.auth.infrastructure.repository;

import com.quertimizer.auth.application.port.VerificationCodeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeInMemoryRepository implements VerificationCodeRepository {

    private final Map<String, String> emailToCodeMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> emailToExpiredAtMap = new ConcurrentHashMap<>();
    private final Set<String> verifiedEmailSet = ConcurrentHashMap.newKeySet();

    @Override
    public void saveCode(String email, String code, LocalDateTime expiredAt) {
        // 인증코드 저장
        emailToCodeMap.put(email, code);
        emailToExpiredAtMap.put(email, expiredAt);
        verifiedEmailSet.remove(email);
    }

    @Override
    public Optional<String> findCode(String email) {
        // 인증코드 조회
        return Optional.ofNullable(emailToCodeMap.get(email));
    }

    @Override
    public Optional<LocalDateTime> findExpiredAt(String email) {
        // Expired At 조회
        return Optional.ofNullable(emailToExpiredAtMap.get(email));
    }

    @Override
    public boolean isVerified(String email) {
        // Verified 여부 확인
        return verifiedEmailSet.contains(email);
    }

    @Override
    public void markVerified(String email) {
        // Verified 처리
        verifiedEmailSet.add(email);
    }

    @Override
    public void clear(String email) {
        // clear 정리
        emailToCodeMap.remove(email);
        emailToExpiredAtMap.remove(email);
        verifiedEmailSet.remove(email);
    }

    @Override
    public void deleteExpired(LocalDateTime now) {
        // Expired 삭제
        emailToExpiredAtMap.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        emailToCodeMap.entrySet().removeIf(entry -> !emailToExpiredAtMap.containsKey(entry.getKey()));
        verifiedEmailSet.removeIf(email -> !emailToExpiredAtMap.containsKey(email));
    }
}
