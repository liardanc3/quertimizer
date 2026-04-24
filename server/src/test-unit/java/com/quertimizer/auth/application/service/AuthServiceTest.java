package com.quertimizer.auth.application.service;

import com.quertimizer.auth.domain.model.LoginFailReason;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.auth.presentation.dto.request.LoginReq;
import com.quertimizer.auth.presentation.dto.request.ResetPasswordReq;
import com.quertimizer.auth.presentation.dto.request.SignupReq;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.infrastructure.repository.UserJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.auth.domain.model.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // 메일 본문에서 6자리 인증코드를 다시 꺼내 서비스 다음 단계를 검증한다.
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("인증코드: ([A-Z0-9]{6})");

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MailService mailService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("유저 저장 + Authentication 반환")
            void saveUserAndReturnAuthentication() {
                // given
                SignupReq request = SignupReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();
                Authentication expectedAuthentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(userRepository.existsByHandle(request.getHandle())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
                when(passwordEncoder.encode(request.getPassword())).thenReturn(Sha512DigestUtils.shaHex(request.getPassword()));
                when(authenticationManager.authenticate(any())).thenReturn(expectedAuthentication);

                // when
                Authentication authentication = authService.signup(request);

                // then
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());

                User savedUser = captor.getValue();
                assertEquals(request.getHandle(), savedUser.getHandle());
                assertEquals(request.getEmail(), savedUser.getEmail());
                assertEquals(Sha512DigestUtils.shaHex(request.getPassword()), savedUser.getPassword());
                assertEquals(expectedAuthentication, authentication);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : handle 중복")
            void throwDuplicatedHandle() {
                // given
                SignupReq request = SignupReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByHandle(request.getHandle())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.signup(request));

                // then
                assertEquals(DUPLICATED_HANDLE.getMessage(), exception.getReason());
                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
                verify(userRepository, never()).save(any(User.class));
                verify(authenticationManager, never()).authenticate(any());
            }

            @Test
            @DisplayName("BusinessException 발생 : email 중복")
            void throwDuplicatedEmail() {
                // given
                SignupReq request = SignupReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByHandle(request.getHandle())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.signup(request));

                // then
                assertEquals(DUPLICATED_EMAIL.getMessage(), exception.getReason());
                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
                verify(userRepository, never()).save(any(User.class));
                verify(authenticationManager, never()).authenticate(any());
            }
        }
    }

    @Nested
    @DisplayName("isDuplicatedHandle")
    class IsDuplicatedHandle {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("false 반환")
            void returnsFalse() {
                // given
                String handle = "tester";

                when(userRepository.existsByHandle(handle)).thenReturn(false);

                // when
                boolean duplicated = authService.isDuplicatedHandle(handle);

                // then
                assertEquals(false, duplicated);
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            @Test
            @DisplayName("true 반환 (handle 중복)")
            void returnsTrueWhenHandleDuplicated() {
                // given
                String handle = "tester";

                when(userRepository.existsByHandle(handle)).thenReturn(true);

                // when
                boolean duplicated = authService.isDuplicatedHandle(handle);

                // then
                assertEquals(true, duplicated);
            }
        }
    }

    @Nested
    @DisplayName("isDuplicatedEmail")
    class IsDuplicatedEmail {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("false 반환")
            void returnsFalse() {
                // given
                String email = "tester@example.com";

                when(userRepository.existsByEmail(email)).thenReturn(false);

                // when
                boolean duplicated = authService.isDuplicatedEmail(email);

                // then
                assertEquals(false, duplicated);
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            @Test
            @DisplayName("true 반환 (email 중복)")
            void returnsTrueWhenEmailDuplicated() {
                // given
                String email = "tester@example.com";

                when(userRepository.existsByEmail(email)).thenReturn(true);

                // when
                boolean duplicated = authService.isDuplicatedEmail(email);

                // then
                assertEquals(true, duplicated);
            }
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("Authentication 반환")
            void returnAuthentication() {
                // given
                LoginReq request = LoginReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .build();
                Authentication expectedAuthentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(authenticationManager.authenticate(any()))
                        .thenReturn(expectedAuthentication);
                when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

                // when
                Authentication authentication = authService.login(request, httpServletRequest);

                // then
                assertEquals(expectedAuthentication, authentication);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : 인증 실패")
            void throwInvalidHandleOrPassword() {
                // given
                LoginReq request = LoginReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .build();

                when(authenticationManager.authenticate(any()))
                        .thenThrow(new BadCredentialsException("bad credentials"));

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request, httpServletRequest));

                // then
                assertEquals(LoginFailReason.INVALID_EMAIL_OR_PASSWORD.getMessage(), exception.getReason());
                assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("비밀번호 변경")
            void changePassword() {
                // given
                AccountRecoveryEmailReq sendCodeRequest = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();
                User user = User.create("tester", "old-password", sendCodeRequest.getEmail());

                when(userRepository.findByEmail(sendCodeRequest.getEmail())).thenReturn(Optional.of(user));
                when(passwordEncoder.encode("a".repeat(128))).thenReturn("new-password");

                authService.sendFindPasswordCode(sendCodeRequest);

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                String verificationCode = extractVerificationCode(textCaptor.getValue());
                AccountRecoveryCodeReq verifyCodeRequest = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .build();
                authService.verifyFindPasswordCode(verifyCodeRequest);

                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .password("a".repeat(128))
                        .build();

                // when
                authService.resetPassword(request);

                // then
                assertEquals("new-password", user.getPassword());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : 인증코드 확인 필요")
            void throwVerificationRequired() {
                // given
                AccountRecoveryEmailReq sendCodeRequest = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();
                User user = User.create("tester", "old-password", sendCodeRequest.getEmail());

                when(userRepository.findByEmail(sendCodeRequest.getEmail())).thenReturn(Optional.of(user));

                authService.sendFindPasswordCode(sendCodeRequest);

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                String verificationCode = extractVerificationCode(textCaptor.getValue());
                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .password("a".repeat(128))
                        .build();

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));

                // then
                assertEquals(PASSWORD_RESET_VERIFICATION_REQUIRED.getMessage(), exception.getReason());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }

    private String extractVerificationCode(String message) {
        // 실제 메일 발송 대신, 캡처한 본문에서 인증코드를 추출해 다음 요청에 재사용한다.
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(message);
        if (!matcher.find()) {
            throw new IllegalStateException("인증코드를 찾을 수 없습니다.");
        }

        return matcher.group(1);
    }
}
