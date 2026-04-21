package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckUserIdReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.ResetPasswordReq;
import com.quertimizer.endpoint.api.dto.request.SetupUserIdReq;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.endpoint.api.dto.response.DuplicateCheckRes;
import com.quertimizer.endpoint.api.dto.response.FindUserIdRes;
import com.quertimizer.endpoint.api.dto.response.SessionMeRes;
import com.quertimizer.endpoint.websocket.handler.SessionWebSocketHandler;
import com.quertimizer.service.UserAccountService;
import com.quertimizer.store.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.quertimizer.constant.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_USER_ID;

@RestController
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final TokenBasedRememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository;
    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final SessionStore sessionStore;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupReq request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {

        // 회원가입 처리, 인증정보 세션 저장
        Authentication authentication = userAccountService.signup(request);
        saveAuthenticationToSession(authentication, httpRequest, httpResponse);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/duplicate-check/userId")
    public ResponseEntity<DuplicateCheckRes> checkDuplicateUserId(@Valid @RequestBody DuplicateCheckUserIdReq request) {

        // userId 중복 확인
        if (userAccountService.isDuplicatedUserId(request.getUserId())) {
            return ResponseEntity.ok(DuplicateCheckRes.duplicated(DUPLICATED_USER_ID));
        }

        return ResponseEntity.ok(DuplicateCheckRes.available());
    }

    @PostMapping("/duplicate-check/email")
    public ResponseEntity<DuplicateCheckRes> checkDuplicateEmail(@Valid @RequestBody DuplicateCheckEmailReq request) {

        // email 중복 확인
        if (userAccountService.isDuplicatedEmail(request.getEmail())) {
            return ResponseEntity.ok(DuplicateCheckRes.duplicated(DUPLICATED_EMAIL));
        }

        return ResponseEntity.ok(DuplicateCheckRes.available());
    }

    @PostMapping("/signup/user-id")
    public ResponseEntity<SessionMeRes> setupUserId(@Valid @RequestBody SetupUserIdReq request,
                                                    Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userAccountService.configureUserId(authentication.getName(), request);
        return ResponseEntity.ok(createAuthenticatedSessionResponse(authentication));
    }

    @PostMapping("/login")
    public ResponseEntity<SessionMeRes> login(@Valid @RequestBody LoginReq request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {

        // 로그인 처리, 인증정보 세션 저장
        Authentication authentication = userAccountService.login(request);
        saveAuthenticationToSession(authentication, httpRequest, httpResponse);
        userAccountService.recordAccess(authentication.getName(), resolveClientIp(httpRequest));

        // remember-me 쿠키 처리
        if (request.isRememberLogin()) {
            rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
        } else {
            rememberMeServices.logout(httpRequest, httpResponse, authentication);
        }

        return ResponseEntity.ok(createAuthenticatedSessionResponse(authentication));
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

        return ResponseEntity.ok(createAuthenticatedSessionResponse(authentication));
    }

    @PostMapping("/find-id/send-code")
    public ResponseEntity<Void> sendFindIdCode(@Valid @RequestBody AccountRecoveryEmailReq request) {

        // 아이디 찾기 인증코드 발송
        userAccountService.sendFindIdCode(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-id/verify-code")
    public ResponseEntity<FindUserIdRes> findUserId(@Valid @RequestBody AccountRecoveryCodeReq request) {

        // 인증코드 확인, userId 반환
        return ResponseEntity.ok(userAccountService.findUserId(request));
    }

    @PostMapping("/find-password/send-code")
    public ResponseEntity<Void> sendFindPasswordCode(@Valid @RequestBody AccountRecoveryEmailReq request) {

        // 비밀번호 찾기 인증코드 발송
        userAccountService.sendFindPasswordCode(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/verify-code")
    public ResponseEntity<Void> verifyFindPasswordCode(@Valid @RequestBody AccountRecoveryCodeReq request) {

        // 인증코드 확인, 비밀번호 재설정 가능 상태 전환
        userAccountService.verifyFindPasswordCode(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordReq request) {

        // 비밀번호 재설정
        userAccountService.resetPassword(request);

        return ResponseEntity.ok().build();
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return httpRequest.getRemoteAddr();
    }

    private void saveAuthenticationToSession(Authentication authentication,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    private SessionMeRes createAuthenticatedSessionResponse(Authentication authentication) {
        return userAccountService.findAuthenticatedUser(authentication.getName())
                .map(user -> SessionMeRes.authenticated(
                        user.getUserId(),
                        !user.hasUserId(),
                        user.getResolvedDefaultDbms(),
                        user.getResolvedRole()
                ))
                .orElseGet(() -> SessionMeRes.authenticated(null, true, null, (String) null));
    }

}
