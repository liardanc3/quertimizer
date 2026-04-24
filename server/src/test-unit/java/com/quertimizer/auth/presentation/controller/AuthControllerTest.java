package com.quertimizer.auth.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.auth.presentation.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckHandleReq;
import com.quertimizer.auth.presentation.dto.request.LoginReq;
import com.quertimizer.auth.presentation.dto.request.ResetPasswordReq;
import com.quertimizer.auth.presentation.dto.request.SignupReq;
import com.quertimizer.global.handler.ApiExceptionHandler;
import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.infrastructure.store.SessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenBasedRememberMeServices rememberMeServices;

    @MockitoBean
    private SessionWebSocketHandler sessionWebSocketHandler;

    @MockitoBean
    private LogFormatter logFormatter;

    @MockitoBean
    private SessionStore sessionStore;

    @Nested
    @DisplayName("/signup")
    class Signup {

        private static final String SIGNUP_URL = "/signup";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("201 Created 반환 + session 저장")
            void createdAndSaveSession() throws Exception {
                // given
                SignupReq request = SignupReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();
                Authentication authentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(authService.signup(any(SignupReq.class))).thenReturn(authentication);

                // when
                ResultActions result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isCreated());
                verify(authService).signup(any(SignupReq.class));
                verify(sessionStore).saveContext(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                SignupReq request = SignupReq.builder()
                        .handle("invalid!")
                        .password("g".repeat(128))
                        .email("not-an-email")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).signup(any(SignupReq.class));
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("/duplicate-check/handle")
    class DuplicateCheckHandle {

        private static final String DUPLICATE_CHECK_HANDLE_URL = "/duplicate-check/handle";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 (사용 가능)")
            void ok() throws Exception {
                // given
                DuplicateCheckHandleReq request = DuplicateCheckHandleReq.builder()
                        .handle("tester")
                        .build();

                when(authService.isDuplicatedHandle("tester")).thenReturn(false);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_HANDLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(authService).isDuplicatedHandle("tester");
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (handle 중복)")
            void conflictWhenHandleDuplicated() throws Exception {
                // given
                DuplicateCheckHandleReq request = DuplicateCheckHandleReq.builder()
                        .handle("tester")
                        .build();

                when(authService.isDuplicatedHandle("tester")).thenReturn(true);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_HANDLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value("이미 사용중인 Handle입니다."));
                verify(authService).isDuplicatedHandle("tester");
            }

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                DuplicateCheckHandleReq request = DuplicateCheckHandleReq.builder()
                        .handle("invalid!")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_HANDLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).isDuplicatedHandle(any(String.class));
            }
        }
    }

    @Nested
    @DisplayName("/duplicate-check/email")
    class DuplicateCheckEmail {

        private static final String DUPLICATE_CHECK_EMAIL_URL = "/duplicate-check/email";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 (사용 가능)")
            void ok() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                when(authService.isDuplicatedEmail("tester@example.com")).thenReturn(false);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(authService).isDuplicatedEmail("tester@example.com");
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (email 중복)")
            void conflictWhenEmailDuplicated() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                when(authService.isDuplicatedEmail("tester@example.com")).thenReturn(true);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value("이미 사용중인 이메일입니다."));
                verify(authService).isDuplicatedEmail("tester@example.com");
            }

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email("not-an-email")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).isDuplicatedEmail(any(String.class));
            }
        }
    }

    @Nested
    @DisplayName("/login")
    class Login {

        private static final String LOGIN_URL = "/login";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + session 저장")
            void okAndSaveSession() throws Exception {
                // given
                LoginReq request = LoginReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .build();

                when(authService.login(any(LoginReq.class), any()))
                        .thenReturn(new UsernamePasswordAuthenticationToken("tester", null, List.of()));
                when(authService.findUser("tester"))
                        .thenReturn(Optional.of(User.create("tester", "a".repeat(128), "tester@example.com")));

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(true))
                        .andExpect(jsonPath("$.handle").value("tester"))
                        .andExpect(jsonPath("$.defaultDbms").value("postgresql"))
                        .andExpect(jsonPath("$.role").value("user"));
                verify(authService).login(any(LoginReq.class), any());
                verify(sessionStore).saveContext(any(), any(), any());
                verify(rememberMeServices).logout(any(), any(), any());
                verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
            }

            @Test
            @DisplayName("200 OK 반환 + session 저장 + 로그인 유지")
            void okAndRememberLogin() throws Exception {
                // given
                LoginReq request = LoginReq.builder()
                        .handle("tester")
                        .password("a".repeat(128))
                        .rememberLogin(true)
                        .build();

                when(authService.login(any(LoginReq.class), any()))
                        .thenReturn(new UsernamePasswordAuthenticationToken("tester", null, List.of()));
                when(authService.findUser("tester"))
                        .thenReturn(Optional.of(User.create("tester", "a".repeat(128), "tester@example.com")));

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(true))
                        .andExpect(jsonPath("$.handle").value("tester"))
                        .andExpect(jsonPath("$.defaultDbms").value("postgresql"))
                        .andExpect(jsonPath("$.role").value("user"));
                verify(authService).login(any(LoginReq.class), any());
                verify(sessionStore).saveContext(any(), any(), any());
                verify(rememberMeServices).loginSuccess(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                LoginReq request = LoginReq.builder()
                        .handle("invalid!")
                        .password("short")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).login(any(LoginReq.class), any());
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("/find-password/send-code")
    class SendFindPasswordCode {

        private static final String SEND_FIND_PASSWORD_CODE_URL = "/find-password/send-code";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + 인증코드 발송 요청 처리")
            void okAndSendCode() throws Exception {
                // given
                AccountRecoveryEmailReq request = AccountRecoveryEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(SEND_FIND_PASSWORD_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(authService).sendFindPasswordCode(any(AccountRecoveryEmailReq.class));
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                AccountRecoveryEmailReq request = AccountRecoveryEmailReq.builder()
                        .email("not-an-email")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(SEND_FIND_PASSWORD_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).sendFindPasswordCode(any(AccountRecoveryEmailReq.class));
            }
        }
    }

    @Nested
    @DisplayName("/find-password/verify-code")
    class VerifyFindPasswordCode {

        private static final String VERIFY_FIND_PASSWORD_CODE_URL = "/find-password/verify-code";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + 인증코드 확인 완료")
            void okAndVerifyCode() throws Exception {
                // given
                AccountRecoveryCodeReq request = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code("ABC123")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(VERIFY_FIND_PASSWORD_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(authService).verifyFindPasswordCode(any(AccountRecoveryCodeReq.class));
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                AccountRecoveryCodeReq request = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code("123")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(VERIFY_FIND_PASSWORD_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).verifyFindPasswordCode(any(AccountRecoveryCodeReq.class));
            }
        }
    }

    @Nested
    @DisplayName("/find-password/reset")
    class ResetPassword {

        private static final String RESET_PASSWORD_URL = "/find-password/reset";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + 비밀번호 변경")
            void okAndResetPassword() throws Exception {
                // given
                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code("ABC123")
                        .password("a".repeat(128))
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(RESET_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(authService).resetPassword(any(ResetPasswordReq.class));
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                ResetPasswordReq request = ResetPasswordReq.builder()
                        .email("tester@example.com")
                        .code("ABC123")
                        .password("short")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(RESET_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(authService, never()).resetPassword(any(ResetPasswordReq.class));
            }
        }
    }

    @Nested
    @DisplayName("/logout")
    class Logout {

        private static final String LOGOUT_URL = "/logout";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + session 정리")
            void okAndClearSession() throws Exception {
                // given
                MockHttpSession session = new MockHttpSession();

                // when
                ResultActions result = mockMvc.perform(post(LOGOUT_URL).session(session));

                // then
                result.andExpect(status().isOk());
                verify(sessionStore).removeSession(session.getId());
                verify(sessionWebSocketHandler).closeSessionSockets(session.getId());
                verify(rememberMeServices, atLeastOnce()).logout(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("/session/me")
    class SessionMe {

        private static final String SESSION_ME_URL = "/session/me";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK + handle 반환 + session 저장")
            void okAndSaveSession() throws Exception {
                // given
                Authentication authentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());
                when(authService.findUser("tester"))
                        .thenReturn(Optional.of(User.create("tester", "a".repeat(128), "tester@example.com")));

                // when
                ResultActions result = mockMvc.perform(post(SESSION_ME_URL).principal(authentication));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(true))
                        .andExpect(jsonPath("$.handle").value("tester"))
                        .andExpect(jsonPath("$.defaultDbms").value("postgresql"))
                        .andExpect(jsonPath("$.role").value("user"));
                verify(sessionStore).saveContext(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("200 OK + false (인증 없음)")
            void okWhenUnauthenticated() throws Exception {
                // when
                ResultActions result = mockMvc.perform(post(SESSION_ME_URL));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(false))
                        .andExpect(jsonPath("$.handle").doesNotExist());
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }

    // 동일한 JSON 전송 방식을 유지하면서 회원가입 요청만 간결하게 재사용한다.
    private ResultActions postSignup(SignupReq request) throws Exception {
        return mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // 동일한 JSON 전송 방식을 유지하면서 로그인 요청만 간결하게 재사용한다.
    private ResultActions postEmailLogin(LoginReq request) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // controller가 SecurityContext를 HttpSession에 실제로 저장했는지 확인한다.
    private void assertSessionAuthenticated(MockHttpSession session, String handle) {
        org.assertj.core.api.Assertions.assertThat(session).isNotNull();

        Object securityContext = session.getAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        org.assertj.core.api.Assertions.assertThat(securityContext).isNotNull();
        org.assertj.core.api.Assertions.assertThat(((org.springframework.security.core.context.SecurityContext) securityContext)
                .getAuthentication()
                .getName()).isEqualTo(handle);
    }
}
