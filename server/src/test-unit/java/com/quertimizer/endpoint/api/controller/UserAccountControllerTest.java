package com.quertimizer.endpoint.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryCodeReq;
import com.quertimizer.endpoint.api.dto.request.AccountRecoveryEmailReq;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckUserIdReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.ResetPasswordReq;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.endpoint.api.dto.response.FindUserIdRes;
import com.quertimizer.endpoint.api.handler.ApiExceptionHandler;
import com.quertimizer.endpoint.websocket.handler.SessionWebSocketHandler;
import com.quertimizer.log.LogFormatter;
import com.quertimizer.service.UserAccountService;
import com.quertimizer.store.SessionStore;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAccountController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAccountService userAccountService;

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
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();
                Authentication authentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                when(userAccountService.signup(any(SignupReq.class))).thenReturn(authentication);

                // when
                ResultActions result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isCreated());
                verify(userAccountService).signup(any(SignupReq.class));
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
                        .userId("invalid!")
                        .password("g".repeat(128))
                        .email("not-an-email")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(userAccountService, never()).signup(any(SignupReq.class));
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("/duplicate-check/userId")
    class DuplicateCheckUserId {

        private static final String DUPLICATE_CHECK_USER_ID_URL = "/duplicate-check/userId";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK + true (사용 가능)")
            void ok() throws Exception {
                // given
                DuplicateCheckUserIdReq request = DuplicateCheckUserIdReq.builder()
                        .userId("tester")
                        .build();

                when(userAccountService.isDuplicatedUserId("tester")).thenReturn(false);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(true))
                        .andExpect(jsonPath("$.reason").doesNotExist());
                verify(userAccountService).isDuplicatedUserId("tester");
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("200 OK + false (userId 중복)")
            void okWhenUserIdDuplicated() throws Exception {
                // given
                DuplicateCheckUserIdReq request = DuplicateCheckUserIdReq.builder()
                        .userId("tester")
                        .build();

                when(userAccountService.isDuplicatedUserId("tester")).thenReturn(true);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(false))
                        .andExpect(jsonPath("$.reason").value("이미 사용중인 아이디입니다."));
                verify(userAccountService).isDuplicatedUserId("tester");
            }

            @Test
            @DisplayName("400 Bad Request 반환 (비정상 파라미터)")
            void badRequestWhenValidationFails() throws Exception {
                // given
                DuplicateCheckUserIdReq request = DuplicateCheckUserIdReq.builder()
                        .userId("invalid!")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(userAccountService, never()).isDuplicatedUserId(any(String.class));
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
            @DisplayName("200 OK + true (사용 가능)")
            void ok() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                when(userAccountService.isDuplicatedEmail("tester@example.com")).thenReturn(false);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(true))
                        .andExpect(jsonPath("$.reason").doesNotExist());
                verify(userAccountService).isDuplicatedEmail("tester@example.com");
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("200 OK + false (email 중복)")
            void okWhenEmailDuplicated() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email("tester@example.com")
                        .build();

                when(userAccountService.isDuplicatedEmail("tester@example.com")).thenReturn(true);

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(false))
                        .andExpect(jsonPath("$.reason").value("이미 사용중인 이메일입니다."));
                verify(userAccountService).isDuplicatedEmail("tester@example.com");
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
                verify(userAccountService, never()).isDuplicatedEmail(any(String.class));
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
                        .userId("tester")
                        .password("a".repeat(128))
                        .build();

                when(userAccountService.login(any(LoginReq.class)))
                        .thenReturn(new UsernamePasswordAuthenticationToken("tester", null, List.of()));

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(userAccountService).login(any(LoginReq.class));
                verify(sessionStore).saveContext(any(), any(), any());
                verify(rememberMeServices).logout(any(), any(), any());
                verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
            }

            @Test
            @DisplayName("200 OK 반환 + session 저장 + 로그인 유지")
            void okAndRememberLogin() throws Exception {
                // given
                LoginReq request = LoginReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .rememberLogin(true)
                        .build();

                when(userAccountService.login(any(LoginReq.class)))
                        .thenReturn(new UsernamePasswordAuthenticationToken("tester", null, List.of()));

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(userAccountService).login(any(LoginReq.class));
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
                        .userId("invalid!")
                        .password("short")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(userAccountService, never()).login(any(LoginReq.class));
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("/find-id/send-code")
    class SendFindIdCode {

        private static final String SEND_FIND_ID_CODE_URL = "/find-id/send-code";

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
                ResultActions result = mockMvc.perform(post(SEND_FIND_ID_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
                verify(userAccountService).sendFindIdCode(any(AccountRecoveryEmailReq.class));
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
                ResultActions result = mockMvc.perform(post(SEND_FIND_ID_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(userAccountService, never()).sendFindIdCode(any(AccountRecoveryEmailReq.class));
            }
        }
    }

    @Nested
    @DisplayName("/find-id/verify-code")
    class FindUserId {

        private static final String FIND_USER_ID_URL = "/find-id/verify-code";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("200 OK 반환 + userId 반환")
            void okAndReturnUserId() throws Exception {
                // given
                AccountRecoveryCodeReq request = AccountRecoveryCodeReq.builder()
                        .email("tester@example.com")
                        .code("ABC123")
                        .build();

                when(userAccountService.findUserId(any(AccountRecoveryCodeReq.class)))
                        .thenReturn(new FindUserIdRes("tester"));

                // when
                ResultActions result = mockMvc.perform(post(FIND_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").value("tester"));
                verify(userAccountService).findUserId(any(AccountRecoveryCodeReq.class));
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
                ResultActions result = mockMvc.perform(post(FIND_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isBadRequest());
                verify(userAccountService, never()).findUserId(any(AccountRecoveryCodeReq.class));
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
                verify(userAccountService).sendFindPasswordCode(any(AccountRecoveryEmailReq.class));
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
                verify(userAccountService, never()).sendFindPasswordCode(any(AccountRecoveryEmailReq.class));
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
                verify(userAccountService).verifyFindPasswordCode(any(AccountRecoveryCodeReq.class));
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
                verify(userAccountService, never()).verifyFindPasswordCode(any(AccountRecoveryCodeReq.class));
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
                verify(userAccountService).resetPassword(any(ResetPasswordReq.class));
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
                verify(userAccountService, never()).resetPassword(any(ResetPasswordReq.class));
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
            @DisplayName("200 OK + userId 반환 + session 저장")
            void okAndSaveSession() throws Exception {
                // given
                Authentication authentication = new UsernamePasswordAuthenticationToken("tester", null, List.of());

                // when
                ResultActions result = mockMvc.perform(post(SESSION_ME_URL).principal(authentication));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(true))
                        .andExpect(jsonPath("$.userId").value("tester"));
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
                        .andExpect(jsonPath("$.userId").doesNotExist());
                verify(sessionStore, never()).saveContext(any(), any(), any());
            }
        }
    }
}
