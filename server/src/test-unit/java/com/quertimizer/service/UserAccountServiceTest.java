package com.quertimizer.service;

import com.quertimizer.constant.LoginFailReason;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.ResetPasswordReq;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.endpoint.api.dto.response.FindUserIdRes;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.UserRepository;
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

import static com.quertimizer.constant.AccountRecoveryFailReason.EMAIL_NOT_FOUND;
import static com.quertimizer.constant.AccountRecoveryFailReason.PASSWORD_RESET_VERIFICATION_REQUIRED;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("인증코드: ([A-Z0-9]{6})");

    @InjectMocks
    private UserAccountService userAccountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MailService mailService;

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
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();
                Authentication expectedAuthentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
                when(passwordEncoder.encode(request.getPassword())).thenReturn(Sha512DigestUtils.shaHex(request.getPassword()));
                when(authenticationManager.authenticate(any())).thenReturn(expectedAuthentication);

                // when
                Authentication authentication = userAccountService.signup(request);

                // then
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());

                User savedUser = captor.getValue();
                assertEquals(request.getUserId(), savedUser.getUserId());
                assertEquals(request.getEmail(), savedUser.getEmail());
                assertEquals(Sha512DigestUtils.shaHex(request.getPassword()), savedUser.getPassword());
                assertEquals(expectedAuthentication, authentication);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : userId 중복")
            void throwDuplicatedUserId() {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userAccountService.signup(request));

                // then
                assertEquals(DUPLICATED_USER_ID, exception.getReason());
                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
                verify(userRepository, never()).save(any(User.class));
                verify(authenticationManager, never()).authenticate(any());
            }

            @Test
            @DisplayName("BusinessException 발생 : email 중복")
            void throwDuplicatedEmail() {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userAccountService.signup(request));

                // then
                assertEquals(DUPLICATED_EMAIL, exception.getReason());
                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
                verify(userRepository, never()).save(any(User.class));
                verify(authenticationManager, never()).authenticate(any());
            }
        }
    }

    @Nested
    @DisplayName("isDuplicatedUserId")
    class IsDuplicatedUserId {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("false 반환")
            void returnsFalse() {
                // given
                String userId = "tester";

                when(userRepository.existsByUserId(userId)).thenReturn(false);

                // when
                boolean duplicated = userAccountService.isDuplicatedUserId(userId);

                // then
                assertEquals(false, duplicated);
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            @Test
            @DisplayName("true 반환 (userId 중복)")
            void returnsTrueWhenUserIdDuplicated() {
                // given
                String userId = "tester";

                when(userRepository.existsByUserId(userId)).thenReturn(true);

                // when
                boolean duplicated = userAccountService.isDuplicatedUserId(userId);

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
                boolean duplicated = userAccountService.isDuplicatedEmail(email);

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
                boolean duplicated = userAccountService.isDuplicatedEmail(email);

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
                        .userId("tester")
                        .password("a".repeat(128))
                        .build();
                Authentication expectedAuthentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(authenticationManager.authenticate(any()))
                        .thenReturn(expectedAuthentication);

                // when
                Authentication authentication = userAccountService.login(request);

                // then
                assertEquals(expectedAuthentication, authentication);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : 인증 실패")
            void throwInvalidUserIdOrPassword() {
                // given
                LoginReq request = LoginReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .build();

                when(authenticationManager.authenticate(any()))
                        .thenThrow(new BadCredentialsException("bad credentials"));

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userAccountService.login(request));

                // then
                assertEquals(LoginFailReason.INVALID_USER_ID_OR_PASSWORD, exception.getReason());
                assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("sendFindIdCode")
    class SendFindIdCode {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("메일 발송")
            void sendMail() {
                // given
                AccountRecoveryEmailReq request = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();
                User user = User.create("tester", "encoded-password", request.getEmail());

                when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

                // when
                userAccountService.sendFindIdCode(request);

                // then
                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                assertTrue(extractVerificationCode(textCaptor.getValue()).matches("^[A-Z0-9]{6}$"));
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : 등록되지 않은 이메일")
            void throwEmailNotFound() {
                // given
                AccountRecoveryEmailReq request = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userAccountService.sendFindIdCode(request));

                // then
                assertEquals(EMAIL_NOT_FOUND, exception.getReason());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                verify(mailService, never()).send(any(String.class), any(String.class), any(String.class));
            }
        }
    }

    @Nested
    @DisplayName("findUserId")
    class FindUserId {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("userId 반환")
            void returnUserId() {
                // given
                AccountRecoveryEmailReq sendCodeRequest = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();
                User user = User.create("tester", "encoded-password", sendCodeRequest.getEmail());

                when(userRepository.findByEmail(sendCodeRequest.getEmail())).thenReturn(Optional.of(user));

                userAccountService.sendFindIdCode(sendCodeRequest);

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                String verificationCode = extractVerificationCode(textCaptor.getValue());
                AccountRecoveryCodeReq request = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .build();

                // when
                FindUserIdRes response = userAccountService.findUserId(request);

                // then
                assertEquals("tester", response.getUserId());
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

                userAccountService.sendFindPasswordCode(sendCodeRequest);

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                String verificationCode = extractVerificationCode(textCaptor.getValue());
                AccountRecoveryCodeReq verifyCodeRequest = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .build();
                userAccountService.verifyFindPasswordCode(verifyCodeRequest);

                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .password("a".repeat(128))
                        .build();

                // when
                userAccountService.resetPassword(request);

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

                userAccountService.sendFindPasswordCode(sendCodeRequest);

                ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
                verify(mailService).send(any(String.class), any(String.class), textCaptor.capture());
                String verificationCode = extractVerificationCode(textCaptor.getValue());
                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code(verificationCode)
                        .password("a".repeat(128))
                        .build();

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userAccountService.resetPassword(request));

                // then
                assertEquals(PASSWORD_RESET_VERIFICATION_REQUIRED, exception.getReason());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }

    private String extractVerificationCode(String message) {
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(message);
        if (!matcher.find()) {
            throw new IllegalStateException("인증코드를 찾을 수 없습니다.");
        }

        return matcher.group(1);
    }
}
