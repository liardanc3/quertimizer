package com.quertimizer.store;

import com.quertimizer.repository.UserSessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SessionStore implements SecurityContextRepository {

    private static final double BYTES_PER_MB = 1024d * 1024d;

    private final UserDetailsService userDetailsService;
    private final UserSessionRepository userSessionRepository;
    private final TaskExecutor taskExecutor;
    private final Map<String, String> userIdsBySessionId = new ConcurrentHashMap<>();
    private final HttpSessionSecurityContextRepository delegate = new HttpSessionSecurityContextRepository();

    public SessionStore(UserDetailsService userDetailsService,
                        UserSessionRepository userSessionRepository,
                        @Qualifier("sessionManagingExecutor") TaskExecutor taskExecutor) {
        this.userDetailsService = userDetailsService;
        this.userSessionRepository = userSessionRepository;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void restoreSessionsFromDatabase() {

        // 서버 재기동 복구용 세션 메모리 적재
        userSessionRepository.findAll()
                .forEach(userSession -> userIdsBySessionId.put(userSession.sessionId(), userSession.userId()));

        log.info("SessionStore 세팅 완료 : {} MB", formatLoadedDataSizeInMb());
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        return loadSecurityContext(requestResponseHolder.getRequest());
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        return new DeferredSecurityContext() {
            private SecurityContext securityContext;
            private boolean loaded;

            @Override
            public SecurityContext get() {
                if (!loaded) {
                    securityContext = loadSecurityContext(request);
                    loaded = true;
                }

                return securityContext;
            }

            @Override
            public boolean isGenerated() {
                return !isAuthenticatedUser(get().getAuthentication());
            }
        };
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {

        // Spring Security 기본 저장
        delegate.saveContext(context, request, response);

        // 복구 대상 세션 동기화
        resolveCurrentSessionId(request).ifPresent(sessionId -> synchronizeStoredSession(sessionId, context.getAuthentication()));
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        if (delegate.containsContext(request)) {
            return true;
        }

        return resolveRestorableSessionId(request)
                .flatMap(this::findUserId)
                .isPresent();
    }

    public void saveSession(String sessionId, String userId) {

        // 세션 메모리 반영
        userIdsBySessionId.put(sessionId, userId);

        // 세션 DB 비동기 반영
        taskExecutor.execute(() -> saveSessionToDatabase(sessionId, userId));
    }

    public Optional<String> findUserId(String sessionId) {
        return Optional.ofNullable(userIdsBySessionId.get(sessionId));
    }

    public void removeSession(String sessionId) {

        // 세션 메모리, DB 제거
        userIdsBySessionId.remove(sessionId);
        taskExecutor.execute(() -> userSessionRepository.deleteById(sessionId));
    }

    private SecurityContext loadSecurityContext(HttpServletRequest request) {

        // 현재 HttpSession 인증 우선 사용
        SecurityContext securityContext = delegate.loadDeferredContext(request).get();
        if (isAuthenticatedUser(securityContext.getAuthentication())) {
            return securityContext;
        }

        // 저장된 세션 기준 인증 복구
        return restoreSecurityContext(request).orElse(securityContext);
    }

    private Optional<SecurityContext> restoreSecurityContext(HttpServletRequest request) {
        return resolveRestorableSessionId(request)
                .flatMap(this::restoreAuthentication)
                .map(authentication -> createRestoredSecurityContext(request, authentication));
    }

    private Optional<Authentication> restoreAuthentication(String sessionId) {
        return findUserId(sessionId)
                .flatMap(userId -> loadAuthentication(sessionId, userId));
    }

    private Optional<Authentication> loadAuthentication(String sessionId, String userId) {
        try {

            // userId 기준 인증 객체 재생성
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails,
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );
            return Optional.of(authentication);
        } catch (UsernameNotFoundException exception) {
            removeSession(sessionId);
            return Optional.empty();
        }
    }

    private SecurityContext createRestoredSecurityContext(HttpServletRequest request, Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        // 복구된 인증을 새 HttpSession에 저장
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        saveSession(session.getId(), authentication.getName());
        removeRequestedSessionIfChanged(request, session.getId());

        return securityContext;
    }

    private Optional<String> resolveCurrentSessionId(HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(false))
                .map(HttpSession::getId);
    }

    private Optional<String> resolveRestorableSessionId(HttpServletRequest request) {

        // 현재 HttpSession 기준 복구 대상 확인
        Optional<String> currentSessionId = resolveCurrentSessionId(request)
                .filter(this::hasStoredSession);
        if (currentSessionId.isPresent()) {
            return currentSessionId;
        }

        // 요청 쿠키의 기존 JSESSIONID 기준 복구 대상 확인
        String requestedSessionId = request.getRequestedSessionId();
        if (requestedSessionId == null || requestedSessionId.isBlank() || !hasStoredSession(requestedSessionId)) {
            return Optional.empty();
        }

        return Optional.of(requestedSessionId);
    }

    private void synchronizeStoredSession(String sessionId, Authentication authentication) {
        if (isAuthenticatedUser(authentication)) {

            // 인증 세션 저장
            saveSession(sessionId, authentication.getName());
            return;
        }

        // 비인증 세션 제거
        removeSession(sessionId);
    }

    private void removeRequestedSessionIfChanged(HttpServletRequest request, String currentSessionId) {
        String requestedSessionId = request.getRequestedSessionId();
        if (requestedSessionId == null || requestedSessionId.isBlank() || requestedSessionId.equals(currentSessionId)) {
            return;
        }

        removeSession(requestedSessionId);
    }

    private boolean hasStoredSession(String sessionId) {
        return findUserId(sessionId).isPresent();
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    private void saveSessionToDatabase(String sessionId, String userId) {

        // 기존 세션 갱신 또는 신규 생성
        userSessionRepository.save(userSessionRepository.findById(sessionId)
                .map(userSession -> {
                    userSession.refresh(userId);
                    return userSession;
                })
                .orElseGet(() -> com.quertimizer.entity.UserSession.create(sessionId, userId)));
    }

    private String formatLoadedDataSizeInMb() {
        double loadedDataSizeInMb = calculateLoadedDataBytes() / BYTES_PER_MB;
        return "%.4f".formatted(loadedDataSizeInMb);
    }

    private long calculateLoadedDataBytes() {
        return userIdsBySessionId.entrySet().stream()
                .mapToLong(entry -> measureString(entry.getKey()) + measureString(entry.getValue()))
                .sum();
    }

    private long measureString(String value) {
        if (value == null) {
            return 0;
        }

        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
