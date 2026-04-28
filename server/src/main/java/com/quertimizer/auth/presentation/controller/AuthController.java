package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.input.SetupHandleInput;
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

    /**
     * 회원가입용 인증코드를 이메일로 전송한다.
     *
     * @param request 인증코드를 받을 이메일 요청
     */
    @PostMapping("/signup/send-code")
    public ResponseEntity<Void> sendSignupCode(@Valid @RequestBody SendCodeReq request) {
        sendSignupCode.execute(SendCodeInput.of(request.getEmail()));
        return ResponseEntity.ok().build();
    }

    /**
     * 회원가입용 인증코드를 검증한다.
     *
     * @param request 인증코드 검증 요청
     */
    @PostMapping("/signup/verify-code")
    public ResponseEntity<Void> verifySignupCode(@Valid @RequestBody VerifyCodeReq request) {
        verifySignupCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode()));
        return ResponseEntity.ok().build();
    }

    /**
     * 이메일 회원가입을 완료하고 생성된 사용자를 현재 세션에 로그인시킨다.
     *
     * <ol>
     *   <li>회원가입 처리
     *   <li>이메일 로그인 인증 결과 생성
     *   <li>인증 결과 저장 후 완료 응답 반환
     * </ol>
     *
     * @param signupReq 회원가입 요청
     * @param httpRequest 클라이언트 IP 조회와 인증 저장소 저장에 사용하는 HTTP 요청
     * @param httpResponse 인증 저장소 저장에 사용하는 HTTP 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupReq signupReq,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        signup.execute(signupReq.toSignupInput());

        String accessIp = authSupport.resolveClientIp(httpRequest);
        EmailLoginInput emailLoginInput = EmailLoginInput.of(signupReq.getEmail(), signupReq.getPassword(), accessIp);
        Authentication authentication = emailLogin.execute(emailLoginInput);

        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 이메일 로그인 결과를 세션과 remember-me 쿠키에 반영하고 부트스트랩 정보를 반환한다.
     *
     * <ol>
     *   <li>이메일 로그인 인증 결과 생성
     *   <li>인증 결과를 SecurityContextRepository에 저장
     *   <li>remember-me 쿠키 저장
     *   <li>사용자 부트스트랩 정보 반환
     * </ol>
     *
     * @param loginReq 이메일 로그인 요청
     * @param httpRequest 클라이언트 IP 조회와 인증 저장소 저장에 사용하는 HTTP 요청
     * @param httpResponse 인증 저장소 저장과 쿠키 저장에 사용하는 HTTP 응답
     */
    @PostMapping("/login")
    public ResponseEntity<UserBootstrapInfoRes> emailLogin(@Valid @RequestBody LoginReq loginReq,
                                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessIp = authSupport.resolveClientIp(httpRequest);
        EmailLoginInput emailLoginInput = EmailLoginInput.of(loginReq.getEmail(), loginReq.getPassword(), accessIp);
        Authentication authentication = emailLogin.execute(emailLoginInput);

        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);

        authSupport.saveRememberMeCookie(authentication, httpRequest, httpResponse);

        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    /**
     * OAuth2 인증 결과로 소셜 로그인을 완료한다.
     *
     * <ol>
     *   <li>Spring Security가 전달한 OAuth2 인증 정보를 서비스 인증 결과로 변환
     *   <li>서비스 인증 결과를 SecurityContextRepository에 저장
     *   <li>provider 정보를 포함한 소셜 로그인 성공 URL로 리다이렉트
     * </ol>
     *
     * @param authentication Spring Security가 생성한 OAuth2 인증 정보
     * @param httpRequest 클라이언트 IP 조회와 인증 저장소 저장에 사용하는 HTTP 요청
     * @param httpResponse 인증 저장소 저장과 리다이렉트에 사용하는 HTTP 응답
     * @throws IOException 소셜 로그인 성공 URL로 리다이렉트하지 못한 경우
     */
    @GetMapping("/login/social/success")
    public void completeSocialLogin(Authentication authentication,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        SocialLoginInput socialLoginInput = SocialLoginInput.of(authentication, authSupport.resolveClientIp(httpRequest));
        Authentication sessionAuthentication = socialLogin.execute(socialLoginInput);

        authSupport.saveAuthenticationToRepository(sessionAuthentication, httpRequest, httpResponse);

        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String url = authSupport.buildSocialLoginSuccessUrl(provider);
        httpResponse.sendRedirect(url);
    }

    /**
     * 회원가입에 사용할 handle 중복 여부를 검증한다.
     *
     * @param request 중복 검증할 handle 요청
     */
    @PostMapping("/duplicate-check/handle")
    public ResponseEntity<Void> checkDuplicateHandle(@Valid @RequestBody DuplicateCheckHandleReq request) {
        validateAvailableHandle.execute(request.getHandle());
        return ResponseEntity.ok().build();
    }

    /**
     * 회원가입에 사용할 이메일 중복 여부를 검증한다.
     *
     * @param request 중복 검증할 이메일 요청
     */
    @PostMapping("/duplicate-check/email")
    public ResponseEntity<Void> checkDuplicateEmail(@Valid @RequestBody DuplicateCheckEmailReq request) {
        validateAvailableEmail.execute(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * 가입 직후 필요한 handle을 설정하고 최신 부트스트랩 정보를 반환한다.
     *
     * <ol>
     *   <li>handle 설정 입력 생성 및 실행
     *   <li>사용자 부트스트랩 정보 반환
     * </ol>
     *
     * @param request 설정할 handle 요청
     * @param authentication 현재 세션 인증 정보
     */
    @PostMapping("/signup/handle")
    public ResponseEntity<UserBootstrapInfoRes> setupHandle(@Valid @RequestBody SetupHandleReq request, Authentication authentication) {
        SetupHandleInput setupHandleInput = SetupHandleInput.of(authentication.getName(), request.getHandle());
        setupHandle.execute(setupHandleInput);

        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    /**
     * 현재 세션의 소켓, remember-me 쿠키, 인증 정보를 정리한다.
     *
     * <ol>
     *   <li>HttpSession에 연결된 WebSocket 종료
     *   <li>remember-me 쿠키와 SecurityContext 정리
     * </ol>
     *
     * @param authentication 현재 세션 인증 정보
     * @param httpRequest 세션 조회와 로그아웃 처리에 사용하는 HTTP 요청
     * @param httpResponse 쿠키 삭제에 사용하는 HTTP 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication,
                                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Optional.ofNullable(httpRequest.getSession(false))
                .map(HttpSession::getId)
                .ifPresent(authSupport::closeSessionSocket);

        authSupport.deleteRememberMeCookie(authentication, httpRequest, httpResponse);
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 세션 인증 상태를 복원하고 화면 부트스트랩 정보를 반환한다.
     *
     * <ol>
     *   <li>미인증 세션은 비로그인 응답 반환
     *   <li>인증 세션은 SecurityContextRepository에 재저장
     *   <li>사용자 부트스트랩 정보 반환
     * </ol>
     *
     * @param authentication 현재 세션 인증 정보
     * @param httpRequest 인증 저장소 저장에 사용하는 HTTP 요청
     * @param httpResponse 인증 저장소 저장에 사용하는 HTTP 응답
     */
    @PostMapping("/session/me")
    public ResponseEntity<UserBootstrapInfoRes> getSession(Authentication authentication,
                                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(UserBootstrapInfoRes.unauthenticated());
        }

        authSupport.saveAuthenticationToRepository(authentication, httpRequest, httpResponse);

        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authentication.getName());
        return ResponseEntity.ok(UserBootstrapInfoRes.from(userBootstrapInfo));
    }

    /**
     * 소셜 로그인 실패 정보를 포함한 프런트엔드 URL로 리다이렉트한다.
     *
     * @param provider 실패한 소셜 로그인 provider
     * @param httpResponse 리다이렉트에 사용하는 HTTP 응답
     * @throws IOException 소셜 로그인 실패 URL로 리다이렉트하지 못한 경우
     */
    @GetMapping("/login/social/failure")
    public void failSocialLogin(@RequestParam(required = false) String provider, HttpServletResponse httpResponse) throws IOException {
        httpResponse.sendRedirect(authSupport.buildSocialLoginFailureUrl(provider));
    }

    /**
     * 비밀번호 찾기 인증코드를 이메일로 전송한다.
     *
     * @param request 인증코드를 받을 이메일 요청
     */
    @PostMapping("/find-password/send-code")
    public ResponseEntity<Void> sendFindPasswordCode(@Valid @RequestBody SendCodeReq request) {
        sendFindPasswordCode.execute(SendCodeInput.of(request.getEmail()));
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 찾기 인증코드를 검증하고 재설정 가능 상태로 전환한다.
     *
     * @param request 인증코드 검증 요청
     */
    @PostMapping("/find-password/verify-code")
    public ResponseEntity<Void> verifyFindPasswordCode(@Valid @RequestBody VerifyCodeReq request) {
        verifyFindPasswordCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode()));
        return ResponseEntity.ok().build();
    }

    /**
     * 인증이 끝난 이메일의 비밀번호를 재설정한다.
     *
     * @param request 비밀번호 재설정 요청
     */
    @PostMapping("/find-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordReq request) {
        resetPassword.execute(ResetPasswordInput.of(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok().build();
    }
}
