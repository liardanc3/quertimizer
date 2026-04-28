package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.usecase.EmailLogin;
import com.quertimizer.auth.application.usecase.GetUserBootstrapInfo;
import com.quertimizer.auth.application.usecase.ResetPassword;
import com.quertimizer.auth.application.usecase.SendFindPasswordCode;
import com.quertimizer.auth.application.usecase.SendSignupCode;
import com.quertimizer.auth.application.usecase.SetupHandle;
import com.quertimizer.auth.application.usecase.Signup;
import com.quertimizer.auth.application.usecase.SocialLogin;
import com.quertimizer.auth.application.usecase.ValidateAvailableEmail;
import com.quertimizer.auth.application.usecase.ValidateAvailableHandle;
import com.quertimizer.auth.application.usecase.VerifyFindPasswordCode;
import com.quertimizer.auth.application.usecase.VerifySignupCode;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckHandleReq;
import com.quertimizer.auth.presentation.dto.request.LoginReq;
import com.quertimizer.auth.presentation.dto.request.ResetPasswordReq;
import com.quertimizer.auth.presentation.dto.request.SendCodeReq;
import com.quertimizer.auth.presentation.dto.request.SetupHandleReq;
import com.quertimizer.auth.presentation.dto.request.SignupReq;
import com.quertimizer.auth.presentation.dto.request.VerifyCodeReq;
import com.quertimizer.auth.presentation.dto.response.UserBootstrapInfoRes;
import com.quertimizer.auth.presentation.support.AuthSupport;
import com.quertimizer.global.util.CanonicalCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Transactional
public class AuthController {

    private final EmailLogin emailLogin;
    private final SocialLogin socialLogin;
    private final Signup signup;
    private final SendSignupCode sendSignupCode;
    private final VerifySignupCode verifySignupCode;
    private final ValidateAvailableHandle validateAvailableHandle;
    private final ValidateAvailableEmail validateAvailableEmail;
    private final SendFindPasswordCode sendFindPasswordCode;
    private final VerifyFindPasswordCode verifyFindPasswordCode;
    private final ResetPassword resetPassword;
    private final SetupHandle setupHandle;
    private final GetUserBootstrapInfo getUserBootstrapInfo;

    private final AuthSupport authSupport;

    @CanonicalCode
    @PostMapping("/signup/send-code")
    public ResponseEntity<Void> sendSignupCode(@Valid @RequestBody SendCodeReq request) {
        // 이메일 가입 인증코드 전송
        sendSignupCode.execute(SendCodeInput.of(request.getEmail()));

        return ResponseEntity.ok().build();
    }

    @CanonicalCode
    @PostMapping("/signup/verify-code")
    public ResponseEntity<Void> verifySignupCode(@Valid @RequestBody VerifyCodeReq request) {
        // 이메일 가입 인증코드 확인
        verifySignupCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode()));

        return ResponseEntity.ok().build();
    }

    @CanonicalCode
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupReq signupReq,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 회원가입
        SignupInput signupInput = SignupInput.of(signupReq.getEmail(), signupReq.getPassword(), signupReq.getCode());
        signup.execute(signupInput);

        // 이메일 로그인 후 인증결과 조회
        EmailLoginInput emailLoginInput = EmailLoginInput.of(signupReq.getEmail(), signupReq.getPassword(), authSupport.resolveClientIp(httpRequest));
        Authentication authentication = emailLogin.execute(emailLoginInput);

        // 인증결과를 인증 저장소에 저장
        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @CanonicalCode
    @PostMapping("/login")
    public ResponseEntity<UserBootstrapInfoRes> emailLogin(@Valid @RequestBody LoginReq loginReq,
                                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 이메일 로그인 후 인증결과 조회
        EmailLoginInput emailLoginInput = EmailLoginInput.of(loginReq.getEmail(), loginReq.getPassword(), authSupport.resolveClientIp(httpRequest));
        Authentication authentication = emailLogin.execute(emailLoginInput);

        // 인증결과를 인증 저장소에 저장
        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);

        // 로그인 유지용 remember-me 쿠키 응답 헤더(Set-Cookie)에 추가
        authSupport.saveRememberMeCookie(authentication, httpRequest, httpResponse);

        // 화면 구성용 유저 부트스트랩 정보 조회 후 반환
        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    @CanonicalCode
    @GetMapping("/login/social/success")
    public void completeSocialLogin(Authentication authentication,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        // 소셜 로그인 후 인증결과 조회
        SocialLoginInput socialLoginInput = SocialLoginInput.of(authentication, authSupport.resolveClientIp(httpRequest));
        Authentication sessionAuthentication = socialLogin.execute(socialLoginInput);

        // 인증결과를 인증 저장소에 저장
        authSupport.saveAuthenticationToRepository(sessionAuthentication, httpRequest, httpResponse);

        // Provider에 따른 성공 페이지 url 생성 후 리다이렉트
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String url = authSupport.buildSocialLoginSuccessUrl(provider);
        httpResponse.sendRedirect(url);
    }


    @CanonicalCode
    @PostMapping("/duplicate-check/handle")
    public ResponseEntity<Void> checkDuplicateHandle(@Valid @RequestBody DuplicateCheckHandleReq request) {
        // Handle 사용 가능 여부 검증
        validateAvailableHandle.execute(request.getHandle());

        return ResponseEntity.ok().build();
    }

    @CanonicalCode
    @PostMapping("/duplicate-check/email")
    public ResponseEntity<Void> checkDuplicateEmail(@Valid @RequestBody DuplicateCheckEmailReq request) {
        // email 사용 가능 여부 검증
        validateAvailableEmail.execute(request.getEmail());

        return ResponseEntity.ok().build();
    }

    @CanonicalCode
    @PostMapping("/signup/handle")
    public ResponseEntity<UserBootstrapInfoRes> setupHandle(@Valid @RequestBody SetupHandleReq request, Authentication authentication) {
        // Handle 설정
        SetupHandleInput setupHandleInput = SetupHandleInput.of(authentication.getName(), request.getHandle());
        setupHandle.execute(setupHandleInput);

        // 화면 구성용 유저 부트스트랩 정보 조회 후 반환
        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    @CanonicalCode
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 로그아웃 대상 세션 연결 정리
        Optional.ofNullable(httpRequest.getSession(false))
                .map(HttpSession::getId)
                .ifPresent(authSupport::closeSessionSocket);

        // 로그인 유지 쿠키 삭제
        authSupport.deleteRememberMeCookie(authentication, httpRequest, httpResponse);

        return ResponseEntity.ok().build();
    }

    @CanonicalCode
    @PostMapping("/session/me")
    public ResponseEntity<UserBootstrapInfoRes> getSession(Authentication authentication,
                                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 인증된 세션이 아니면 부트스트랩 정보 비워서 반환
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(UserBootstrapInfoRes.unauthenticated());
        }

        // remember-me 복원 뒤 새 세션에 인증정보 재저장
        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);

        // 화면 구성용 유저 부트스트랩 정보 조회 후 반환
        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    @CanonicalCode
    @GetMapping("/login/social/failure")
    public void failSocialLogin(@RequestParam(required = false) String provider, HttpServletResponse httpResponse) throws IOException {
        // Provider에 따른 실패 페이지 url 생성 후 리다이렉트
        httpResponse.sendRedirect(authSupport.buildSocialLoginFailureUrl(provider));
    }

    @PostMapping("/find-password/send-code")
    public ResponseEntity<Void> sendFindPasswordCode(@Valid @RequestBody SendCodeReq request) {
        // 비밀번호 찾기 인증코드 발송
        sendFindPasswordCode.execute(SendCodeInput.of(request.getEmail()));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/verify-code")
    public ResponseEntity<Void> verifyFindPasswordCode(@Valid @RequestBody VerifyCodeReq request) {
        // 인증코드 확인 후 비밀번호 재설정 가능 상태로 전환
        verifyFindPasswordCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode()));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordReq request) {
        // 비밀번호 재설정
        resetPassword.execute(ResetPasswordInput.of(request.getEmail(), request.getPassword()));

        return ResponseEntity.ok().build();
    }
}
