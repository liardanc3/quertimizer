package com.quertimizer.auth.infrastructure.store;

import com.quertimizer.auth.infrastructure.repository.UserSessionRepository;
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
    private final Map<String, String> handlesBySessionId = new ConcurrentHashMap<>();
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
                .forEach(userSession -> handlesBySessionId.put(userSession.sessionId(), userSession.handle()));

        log.info("SessionStore 세팅 완료 : {} MB", formatLoadedDataSizeInMb());
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        // SecurityContextRepository 계약상 즉시 조회가 필요한 경우 동일한 복구 로직을 사용한다.
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
                    // 실제로 인증 정보가 필요한 시점까지 세션 복구 비용을 늦춘다.
                    securityContext = loadSecurityContext(request);
                    loaded = true;
                }

                return securityContext;
            }

            @Override
            public boolean isGenerated() {
                // 인증이 복구되지 않은 경우에만 새 context 로 간주한다.
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

        // HttpSession이 비어 있어도 DB에 복구 가능한 세션이 있으면 true를 반환한다.
        return resolveRestorableSessionId(request)
                .flatMap(this::findHandle)
                .isPresent();
    }

    public void saveSession(String sessionId, String handle) {

        // 세션 메모리 반영
        handlesBySessionId.put(sessionId, handle);

        // 세션 DB 비동기 반영
        taskExecutor.execute(() -> saveSessionToDatabase(sessionId, handle));
    }

    public Optional<String> findHandle(String sessionId) {
        // remember-me나 서버 재기동 후 세션 복구 시 저장된 handle를 찾는다.
        return Optional.ofNullable(handlesBySessionId.get(sessionId));
    }

    public void removeSession(String sessionId) {

        // 세션 메모리, DB 제거
        handlesBySessionId.remove(sessionId);
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
        // 저장된 sessionId에서 handle를 찾은 뒤 Security 인증 객체를 다시 만든다.
        return findHandle(sessionId)
                .flatMap(handle -> loadAuthentication(sessionId, handle));
    }

    private Optional<Authentication> loadAuthentication(String sessionId, String handle) {
        try {

            // handle 기준 인증 객체 재생성
            UserDetails userDetails = userDetailsService.loadUserByUsername(handle);
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
        // 현재 요청에 이미 연결된 HttpSession이 있으면 그 세션을 우선 사용한다.
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
        // 메모리 복구 맵에 남아 있어야 세션 복구 대상으로 인정한다.
        return findHandle(sessionId).isPresent();
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        // anonymousUser는 실제 로그인 세션으로 취급하지 않는다.
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    private void saveSessionToDatabase(String sessionId, String handle) {

        // 기존 세션 갱신 또는 신규 생성
        userSessionRepository.save(userSessionRepository.findById(sessionId)
                .map(userSession -> {
                    userSession.refresh(handle);
                    return userSession;
                })
                .orElseGet(() -> com.quertimizer.auth.domain.entity.UserSession.create(sessionId, handle)));
    }

    private String formatLoadedDataSizeInMb() {
        // 서버 기동 로그에서 복구된 세션 데이터의 대략적인 메모리 사용량을 보여준다.
        double loadedDataSizeInMb = calculateLoadedDataBytes() / BYTES_PER_MB;
        return "%.4f".formatted(loadedDataSizeInMb);
    }

    private long calculateLoadedDataBytes() {
        return handlesBySessionId.entrySet().stream()
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
