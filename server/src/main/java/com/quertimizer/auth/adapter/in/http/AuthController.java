package com.quertimizer.auth.adapter.in.http;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.output.AuthenticatedUserOutput;
import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.auth.application.port.in.EmailLoginUseCase;
import com.quertimizer.auth.application.port.in.GetUserBootstrapInfoUseCase;
import com.quertimizer.auth.application.port.in.ResetPasswordUseCase;
import com.quertimizer.auth.application.port.in.SendFindPasswordCodeUseCase;
import com.quertimizer.auth.application.port.in.SendSignupCodeUseCase;
import com.quertimizer.auth.application.port.in.SetupHandleUseCase;
import com.quertimizer.auth.application.port.in.SignupUseCase;
import com.quertimizer.auth.application.port.in.SocialLoginUseCase;
import com.quertimizer.auth.application.port.in.ValidateAvailableEmailUseCase;
import com.quertimizer.auth.application.port.in.ValidateAvailableHandleUseCase;
import com.quertimizer.auth.application.port.in.VerifyFindPasswordCodeUseCase;
import com.quertimizer.auth.application.port.in.VerifySignupCodeUseCase;
import com.quertimizer.auth.adapter.in.http.request.DuplicateCheckEmailReq;
import com.quertimizer.auth.adapter.in.http.request.DuplicateCheckHandleReq;
import com.quertimizer.auth.adapter.in.http.request.LoginReq;
import com.quertimizer.auth.adapter.in.http.request.ResetPasswordReq;
import com.quertimizer.auth.adapter.in.http.request.SendCodeReq;
import com.quertimizer.auth.adapter.in.http.request.SetupHandleReq;
import com.quertimizer.auth.adapter.in.http.request.SignupReq;
import com.quertimizer.auth.adapter.in.http.request.VerifyCodeReq;
import com.quertimizer.auth.adapter.in.http.response.UserBootstrapInfoRes;
import com.quertimizer.auth.adapter.in.http.support.AuthSupport;
import com.quertimizer.global.exception.BusinessException;
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

import static com.quertimizer.auth.domain.model.AuthFailReason.OAUTH2_AUTHENTICATION_NOT_FOUND;

@RestController
@RequiredArgsConstructor
@Transactional
public class AuthController {

    private final EmailLoginUseCase emailLogin;
    private final SocialLoginUseCase socialLogin;
    private final SignupUseCase signup;
    private final SendSignupCodeUseCase sendSignupCode;
    private final VerifySignupCodeUseCase verifySignupCode;
    private final ValidateAvailableHandleUseCase validateAvailableHandle;
    private final ValidateAvailableEmailUseCase validateAvailableEmail;
    private final SendFindPasswordCodeUseCase sendFindPasswordCode;
    private final VerifyFindPasswordCodeUseCase verifyFindPasswordCode;
    private final ResetPasswordUseCase resetPassword;
    private final SetupHandleUseCase setupHandle;
    private final GetUserBootstrapInfoUseCase getUserBootstrapInfo;

    private final AuthSupport authSupport;

    /**
     * 회원가입용 인증코드를 이메일로 전송한다.
     *
     * @param request 인증코드를 받을 이메일 요청
     */
    @PostMapping("/signup/send-code")
    public ResponseEntity<Void> sendSignupCode(@Valid @RequestBody SendCodeReq request, HttpServletRequest httpRequest) {
        sendSignupCode.execute(SendCodeInput.of(request.getEmail(), authSupport.resolveClientIp(httpRequest)));
        return ResponseEntity.ok().build();
    }

    /**
     * 회원가입용 인증코드를 검증한다.
     *
     * @param request 인증코드 검증 요청
     */
    @PostMapping("/signup/verify-code")
    public ResponseEntity<Void> verifySignupCode(@Valid @RequestBody VerifyCodeReq request, HttpServletRequest httpRequest) {
        verifySignupCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode(), authSupport.resolveClientIp(httpRequest)));
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
        AuthenticatedUserOutput authenticatedUser = emailLogin.execute(emailLoginInput);

        authSupport.saveAuthenticationToRepository(authenticatedUser, httpRequest, httpResponse);
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
        AuthenticatedUserOutput authenticatedUser = emailLogin.execute(emailLoginInput);

        authSupport.saveAuthenticationToRepository(authenticatedUser, httpRequest, httpResponse);

        authSupport.saveRememberMeCookie(authenticatedUser, httpRequest, httpResponse);

        UserBootstrapOutput userBootstrapInfo = getUserBootstrapInfo.execute(authenticatedUser.getEmail());
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
        OAuth2AuthenticationToken oauth2Authentication = resolveOAuth2Authentication(authentication);
        SocialLoginInput socialLoginInput = SocialLoginInput.of(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getPrincipal().getAttributes(),
                authSupport.resolveClientIp(httpRequest)
        );
        AuthenticatedUserOutput authenticatedUser = socialLogin.execute(socialLoginInput);

        authSupport.saveAuthenticationToRepository(authenticatedUser, httpRequest, httpResponse);

        String provider = oauth2Authentication.getAuthorizedClientRegistrationId();
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
     * 현재 세션의 WebSocket 연결, remember-me 쿠키, 인증 정보를 정리한다.
     *
     * <ol>
     *   <li>HttpSession에 연결된 WebSocket 세션 종료
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
                .ifPresent(authSupport::closeSessionWebSockets);

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

    private OAuth2AuthenticationToken resolveOAuth2Authentication(Authentication authentication) {
        // 소셜 로그인 성공 endpoint는 OAuth2AuthenticationToken만 허용
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return oauth2Authentication;
        }

        throw new BusinessException(OAUTH2_AUTHENTICATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
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
    public ResponseEntity<Void> sendFindPasswordCode(@Valid @RequestBody SendCodeReq request, HttpServletRequest httpRequest) {
        sendFindPasswordCode.execute(SendCodeInput.of(request.getEmail(), authSupport.resolveClientIp(httpRequest)));
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 찾기 인증코드를 검증하고 재설정 가능 상태로 전환한다.
     *
     * @param request 인증코드 검증 요청
     */
    @PostMapping("/find-password/verify-code")
    public ResponseEntity<Void> verifyFindPasswordCode(@Valid @RequestBody VerifyCodeReq request, HttpServletRequest httpRequest) {
        verifyFindPasswordCode.execute(VerifyCodeInput.of(request.getEmail(), request.getCode(), authSupport.resolveClientIp(httpRequest)));
        return ResponseEntity.ok().build();
    }

    /**
     * 인증이 끝난 이메일의 비밀번호를 재설정한다.
     *
     * @param request 비밀번호 재설정 요청
     */
    @PostMapping("/find-password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordReq request, HttpServletRequest httpRequest) {
        resetPassword.execute(ResetPasswordInput.of(request.getEmail(), request.getPassword(), authSupport.resolveClientIp(httpRequest)));
        return ResponseEntity.ok().build();
    }
}
