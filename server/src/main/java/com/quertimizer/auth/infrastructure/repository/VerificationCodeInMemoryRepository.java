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

    /**
     * 이메일 인증코드와 만료 시각을 저장하고 기존 인증 완료 상태를 초기화한다.
     *
     * @param email 인증코드를 저장할 이메일
     * @param code 저장할 인증코드
     * @param expiredAt 인증코드 만료 시각
     */
    @Override
    public void saveCode(String email, String code, LocalDateTime expiredAt) {
        emailToCodeMap.put(email, code);
        emailToExpiredAtMap.put(email, expiredAt);
        verifiedEmailSet.remove(email);
    }

    /**
     * 이메일 기준 인증코드를 조회한다.
     *
     * @param email 인증코드를 조회할 이메일
     */
    @Override
    public Optional<String> findCode(String email) {
        return Optional.ofNullable(emailToCodeMap.get(email));
    }

    /**
     * 이메일 기준 인증코드 만료 시각을 조회한다.
     *
     * @param email 만료 시각을 조회할 이메일
     */
    @Override
    public Optional<LocalDateTime> findExpiredAt(String email) {
        return Optional.ofNullable(emailToExpiredAtMap.get(email));
    }

    /**
     * 이메일 인증 완료 여부를 확인한다.
     *
     * @param email 인증 완료 여부를 확인할 이메일
     */
    @Override
    public boolean isVerified(String email) {
        return verifiedEmailSet.contains(email);
    }

    /**
     * 이메일을 인증 완료 상태로 표시한다.
     *
     * @param email 인증 완료로 표시할 이메일
     */
    @Override
    public void markVerified(String email) {
        verifiedEmailSet.add(email);
    }

    /**
     * 이메일 인증코드, 만료 시각, 인증 완료 상태를 모두 제거한다.
     *
     * @param email 인증 상태를 제거할 이메일
     */
    @Override
    public void clear(String email) {
        emailToCodeMap.remove(email);
        emailToExpiredAtMap.remove(email);
        verifiedEmailSet.remove(email);
    }

    /**
     * 만료된 인증코드와 연결된 인증 상태를 제거한다.
     *
     * @param now 만료 여부를 판단할 현재 시각
     */
    @Override
    public void deleteExpired(LocalDateTime now) {
        emailToExpiredAtMap.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        emailToCodeMap.entrySet().removeIf(entry -> !emailToExpiredAtMap.containsKey(entry.getKey()));
        verifiedEmailSet.removeIf(email -> !emailToExpiredAtMap.containsKey(email));
    }
}
