package com.quertimizer.global.filter;

import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.global.support.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final LogFormatter logFormatter;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.logging.api.include-bodies:false}")
    private boolean includeBodies;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청/응답 본문을 후처리에서도 읽을 수 있도록 caching wrapper 적용
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 1024 * 64);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String actor = resolveActor(request);
        String prefix = logFormatter.prefix(actor);

        // 요청 라인과 쿼리스트링 로그 기록
        log.info("{}", logFormatter.formatHttpLine(
                actor,
                "API Request",
                requestWrapper.getMethod() + " " + requestPath(requestWrapper)
        ));
        logLines(logFormatter.formatQueryStringLines(prefix, requestWrapper.getQueryString()));

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {

            // 요청 본문과 응답 결과 로그 기록
            if (includeBodies && shouldLogBody(requestWrapper.getRequestURI(), requestWrapper.getContentType(), requestWrapper.getContentLengthLong())) {
                logLines(logFormatter.formatRequestBodyLines(prefix, extractRequestBody(requestWrapper)));
            }
            log.info("{}", logFormatter.formatHttpLine(actor, "API Response", responseStatus(responseWrapper)));
            if (includeBodies && shouldLogBody(requestWrapper.getRequestURI(), responseWrapper.getContentType(), responseWrapper.getContentAsByteArray().length)) {
                logLines(logFormatter.formatResponseBodyLines(prefix, extractResponseBody(responseWrapper)));
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 필터 제외 여부 확인
        return request.getRequestURI().startsWith("/ws/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private String resolveActor(HttpServletRequest request) {
        // 현재 SecurityContext 인증정보 우선 사용
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }

        // 세션에 저장된 인증정보가 있으면 동일하게 사용자 식별
        Object context = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context instanceof SecurityContext securityContext) {
            Authentication sessionAuthentication = securityContext.getAuthentication();
            if (sessionAuthentication != null && sessionAuthentication.isAuthenticated()) {
                return sessionAuthentication.getName();
            }
        }

        return clientIpResolver.resolve(request);
    }

    private String requestPath(HttpServletRequest request) {
        // 요청 경로 조회
        return request.getRequestURI();
    }

    private String responseStatus(HttpServletResponse response) {
        // 응답 상태 조회
        HttpStatus httpStatus = HttpStatus.resolve(response.getStatus());
        if (httpStatus == null) {
            return Integer.toString(response.getStatus());
        }

        return response.getStatus() + " " + httpStatus.getReasonPhrase();
    }

    private String extractRequestBody(ContentCachingRequestWrapper request) {
        // 요청 본문 추출
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        if (!isTextPayload(request.getContentType())) {
            return "<binary " + content.length + " bytes>";
        }

        return new String(content, resolveCharset(request.getContentType(), request.getCharacterEncoding()));
    }

    private String extractResponseBody(ContentCachingResponseWrapper response) {
        // 응답 본문 추출
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        if (!isTextPayload(response.getContentType())) {
            return "<binary " + content.length + " bytes>";
        }

        return new String(content, resolveCharset(response.getContentType(), response.getCharacterEncoding()));
    }

    private boolean isTextPayload(String contentType) {
        // 텍스트 페이로드 여부 확인
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String normalizedContentType = contentType.toLowerCase();
        return normalizedContentType.contains("json")
                || normalizedContentType.contains("xml")
                || normalizedContentType.contains("text")
                || normalizedContentType.contains("javascript")
                || normalizedContentType.contains("x-www-form-urlencoded");
    }

    private boolean shouldLogBody(String requestUri, String contentType, long contentLength) {
        if (isSensitiveEndpoint(requestUri)) {
            return false;
        }

        if (contentLength > 1024 * 64L) {
            return false;
        }

        return contentType == null || !contentType.toLowerCase().startsWith("multipart/");
    }

    private boolean isSensitiveEndpoint(String requestUri) {
        return requestUri != null && (
                requestUri.equals("/login")
                        || requestUri.startsWith("/signup")
                        || requestUri.startsWith("/find-password")
                        || requestUri.startsWith("/duplicate-check")
        );
    }

    private Charset resolveCharset(String contentType, String characterEncoding) {
        // 문자셋 결정

        Charset contentTypeCharset = extractCharset(contentType);
        if (contentTypeCharset != null) {
            return contentTypeCharset;
        }

        // 문자셋 정보가 없으면 텍스트 계열 payload는 UTF-8 기준으로 처리
        if (characterEncoding == null || characterEncoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        if (StandardCharsets.ISO_8859_1.name().equalsIgnoreCase(characterEncoding) && isUtf8PreferredPayload(contentType)) {
            return StandardCharsets.UTF_8;
        }

        return Charset.forName(characterEncoding);
    }

    private Charset extractCharset(String contentType) {
        // 문자셋 추출
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        for (String contentTypePart : contentType.split(";")) {
            String trimmedPart = contentTypePart.trim();
            if (!trimmedPart.toLowerCase().startsWith("charset=")) {
                continue;
            }

            return Charset.forName(trimmedPart.substring("charset=".length()).trim());
        }

        return null;
    }

    private boolean isUtf8PreferredPayload(String contentType) {
        // UTF-8 우선 페이로드 여부 확인
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String normalizedContentType = contentType.toLowerCase();
        return normalizedContentType.contains("json")
                || normalizedContentType.contains("xml")
                || normalizedContentType.contains("text")
                || normalizedContentType.contains("javascript")
                || normalizedContentType.contains("x-www-form-urlencoded");
    }

    private void logLines(List<String> logLines) {
        // 로그 라인 생성
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }

}
