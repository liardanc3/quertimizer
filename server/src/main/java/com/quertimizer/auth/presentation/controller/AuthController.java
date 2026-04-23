package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.usecase.CheckDuplicateEmail;
import com.quertimizer.auth.application.usecase.CheckDuplicateHandle;
import com.quertimizer.auth.application.usecase.EmailLogin;
import com.quertimizer.auth.application.usecase.FindHandle;
import com.quertimizer.auth.application.usecase.GetAuthenticatedSession;
import com.quertimizer.auth.application.usecase.ResetPassword;
import com.quertimizer.auth.application.usecase.SendFindHandleCode;
import com.quertimizer.auth.application.usecase.SendFindPasswordCode;
import com.quertimizer.auth.application.usecase.SetupHandle;
import com.quertimizer.auth.application.usecase.Signup;
import com.quertimizer.auth.application.usecase.SocialLogin;
import com.quertimizer.auth.application.usecase.VerifyFindPasswordCode;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckHandleReq;
import com.quertimizer.auth.presentation.dto.request.LoginReq;
import com.quertimizer.auth.presentation.dto.request.ResetPasswordReq;
import com.quertimizer.auth.presentation.dto.request.SetupHandleReq;
import com.quertimizer.auth.presentation.dto.request.SignupReq;
import com.quertimizer.auth.presentation.dto.response.DuplicateCheckRes;
import com.quertimizer.auth.presentation.dto.response.FindHandleRes;
import com.quertimizer.auth.presentation.dto.response.SessionMeRes;
import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
import com.quertimizer.auth.infrastructure.store.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.quertimizer.auth.domain.model.AuthFailReason.OAUTH2_AUTHENTICATION_NOT_FOUND;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final EmailLogin emailLogin;
    private final SocialLogin socialLogin;
    private final Signup signup;
    private final CheckDuplicateHandle checkDuplicateHandle;
    private final CheckDuplicateEmail checkDuplicateEmail;
    private final SendFindHandleCode sendFindHandleCode;
    private final FindHandle findHandle;
    private final SendFindPasswordCode sendFindPasswordCode;
    private final VerifyFindPasswordCode verifyFindPasswordCode;
    private final ResetPassword resetPassword;
    private final SetupHandle setupHandle;
    private final GetAuthenticatedSession getAuthenticatedSession;
    private final TokenBasedRememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository;
    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final SessionStore sessionStore;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupReq signupReq,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        // 회원가입 처리, 인증정보 세션 저장
        Authentication authentication = signup.execute(new SignupInput(signupReq.getEmail(), signupReq.getPassword()));
        saveAuthenticationToSession(authentication, httpRequest, httpResponse);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<SessionMeRes> emailLogin(@Valid @RequestBody LoginReq loginReq,
                                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 이메일 로그인 Input 생성
        EmailLoginInput emailLoginInput = EmailLoginInput.of(loginReq.getEmail(), loginReq.getPassword(), resolveClientIp(httpRequest));

        // 이메일 로그인(인증정보 조회)
        Authentication authentication = emailLogin.execute(emailLoginInput);

        // 인증정보 세션에 저장
        saveAuthenticationToSession(authentication, httpRequest, httpResponse);

        // 로그인 유지용 remember-me 쿠키 응답 헤더(Set-Cookie)에 추가
        rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);

        return ResponseEntity.ok(SessionMeRes.from(
                getAuthenticatedSession.execute(authentication.getName())
        ));
    }

    @GetMapping("/login/social/success")
    public void completeSocialLogin(Authentication authentication,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

        // Spring Security가 만든 OAuth2 인증정보에서 provider와 attribute를 꺼내
        // 우리 서비스 기준의 인증 객체로 다시 변환
        OAuth2AuthenticationToken oauth2Authentication = resolveOAuth2Authentication(authentication);
        Authentication sessionAuthentication = socialLogin.execute(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getPrincipal().getAttributes(),
                httpRequest
        );

        // 소셜 로그인도 일반 로그인과 동일하게 세션/접근기록을 남긴 뒤
        // 프런트가 socialLoginSuccess 쿼리로 후처리할 수 있게 redirect
        saveAuthenticationToSession(sessionAuthentication, httpRequest, httpResponse);
        httpResponse.sendRedirect(buildSocialLoginSuccessUrl(oauth2Authentication.getAuthorizedClientRegistrationId()));
    }

    @PostMapping("/duplicate-check/handle")
    public ResponseEntity<DuplicateCheckRes> checkDuplicateHandle(@Valid @RequestBody DuplicateCheckHandleReq request) {

        // handle 중복 확인
        if (checkDuplicateHandle.execute(request.getHandle())) {
            return ResponseEntity.ok(DuplicateCheckRes.duplicated(DUPLICATED_HANDLE.getMessage()));
        }

        return ResponseEntity.ok(DuplicateCheckRes.available());
    }

    @PostMapping("/duplicate-check/email")
    public ResponseEntity<DuplicateCheckRes> checkDuplicateEmail(@Valid @RequestBody DuplicateCheckEmailReq request) {

        // email 중복 확인
        if (checkDuplicateEmail.execute(request.getEmail())) {
            return ResponseEntity.ok(DuplicateCheckRes.duplicated(DUPLICATED_EMAIL.getMessage()));
        }

        return ResponseEntity.ok(DuplicateCheckRes.available());
    }

    @PostMapping("/signup/handle")
    public ResponseEntity<SessionMeRes> setupHandle(@Valid @RequestBody SetupHandleReq request,
                                                    Authentication authentication) {
        // 이미 로그인된 사용자인지 먼저 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Handle 설정 이후 최신 세션 상태를 다시 내려줌
        setupHandle.execute(authentication.getName(), request.toSetupHandleInput());
        return ResponseEntity.ok(SessionMeRes.from(
                getAuthenticatedSession.execute(authentication.getName())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse,
                                       Authentication authentication) {
        HttpSession session = httpRequest.getSession(false);

        // 인증정보, 세션 연결 정리
        if (session != null) {
            sessionStore.removeSession(session.getId());
            sessionWebSocketHandler.closeSessionSockets(session.getId());
        }

        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/me")
    public ResponseEntity<SessionMeRes> getSession(HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse,
                                                   Authentication authentication) {

        // 인증 세션 없음
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(SessionMeRes.unauthenticated());
        }

        // remember-me 복원 뒤 새 세션에 인증정보 재저장
        saveAuthenticationToSession(authentication, httpRequest, httpResponse);

        return ResponseEntity.ok(SessionMeRes.from(
                getAuthenticatedSession.execute(authentication.getName())
        ));
    }



    @GetMapping("/login/social/failure")
    public void failSocialLogin(@RequestParam(required = false) String provider,
                                HttpServletResponse httpResponse) throws IOException {

        // 실패 provider 정보를 유지한 채 프런트로 돌려보내
        // 화면에서 provider별 에러문구를 보여줄 수 있게 함
        httpResponse.sendRedirect(buildSocialLoginFailureUrl(provider));
    }

    @PostMapping("/find-handle/send-code")
    public ResponseEntity<Void> sendFindHandleCode(@Valid @RequestBody AccountRecoveryEmailReq request) {

        // Handle 찾기 인증코드 발송
        sendFindHandleCode.execute(request.toAccountRecoveryEmailInput());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-handle/verify-code")
    public ResponseEntity<FindHandleRes> findHandle(@Valid @RequestBody AccountRecoveryCodeReq request) {

        // 인증코드 확인, handle 반환
        return ResponseEntity.ok(FindHandleRes.from(findHandle.execute(request.toAccountRecoveryCodeInput())));
    }

    @PostMapping("/find-password/send-code")
    public ResponseEntity<Void> sendFindPasswordCode(@Valid @RequestBody AccountRecoveryEmailReq request) {

        // 비밀번호 찾기 인증코드 발송
        sendFindPasswordCode.execute(request.toAccountRecoveryEmailInput());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/verify-code")
    public ResponseEntity<Void> verifyFindPasswordCode(@Valid @RequestBody AccountRecoveryCodeReq request) {

        // 인증코드 확인, 비밀번호 재설정 가능 상태 전환
        verifyFindPasswordCode.execute(request.toAccountRecoveryCodeInput());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordReq request) {

        // 비밀번호 재설정
        resetPassword.execute(request.toResetPasswordInput());

        return ResponseEntity.ok().build();
    }

    private void saveAuthenticationToSession(Authentication authentication,
                                             HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 인증 성공 직후 SecurityContext를 새로 만들어
        // 현재 요청 thread와 세션 저장소 양쪽에 모두 반영
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    private OAuth2AuthenticationToken resolveOAuth2Authentication(Authentication authentication) {
        // 소셜 로그인 성공 endpoint는 OAuth2AuthenticationToken만 정상 입력으로 받는다
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return oauth2Authentication;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, OAUTH2_AUTHENTICATION_NOT_FOUND.getMessage());
    }

    private String buildSocialLoginSuccessUrl(String provider) {
        // 프런트가 provider별 성공 후처리를 할 수 있게 query param 유지
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginSuccess", provider)
                .build()
                .toUriString();
    }

    private String buildSocialLoginFailureUrl(String provider) {
        // provider를 모를 때도 프런트가 공통 에러문구를 만들 수 있도록 oauth2 기본값 사용
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginError", provider == null || provider.isBlank() ? "oauth2" : provider)
                .build()
                .toUriString();
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {
        // 프록시 환경에서는 X-Forwarded-For의 첫 번째 값을 실제 접속 IP로 사용
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        // 프록시 정보가 없으면 직접 연결된 remote address 사용
        return httpRequest.getRemoteAddr();
    }

}
