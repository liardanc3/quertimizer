package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.port.AuthMailSender;
import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.auth.application.port.VerificationCodeRepository;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.Value;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIntegrationTest.AuthControllerTestConfiguration.class)
@DisplayName("AuthController")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BlockedIpRepository blockedIpRepository;
    @Autowired private VerificationCodeRepository verificationCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FakeAuthMailSender authMailSender;

    @BeforeEach
    void setUp() {
        authMailSender.reset();
    }

    @Nested
    @DisplayName("POST /signup/send-code")
    class SendSignupCode {

        @Test
        @DisplayName("성공 (인증코드 저장)")
        void successWhenEmailIsValid() throws Exception {
            // given
            String email = uniqueEmail();
            String requestBody = emailJson(email);

            // when
            var result = mockMvc.perform(post("/signup/send-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            assertThat(verificationCodeRepository.findCode(email)).isPresent();
            assertThat(authMailSender.sentMails()).extracting(SentMail::to).contains(email);
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenEmailInvalid() throws Exception {
            // given
            String requestBody = "{\"email\":\"bad\"}";

            // when
            var result = mockMvc.perform(post("/signup/send-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (차단 IP)")
        void forbiddenWhenIpBlocked() throws Exception {
            // given
            String ipAddress = "10.10.10.10";
            blockedIpRepository.save(BlockedIp.create(ipAddress, null));
            String requestBody = emailJson(uniqueEmail());
            RequestPostProcessor remoteAddress = request -> {
                request.setRemoteAddr(ipAddress);
                return request;
            };

            // when
            var result = mockMvc.perform(post("/signup/send-code").with(csrf())
                    .with(remoteAddress)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Throw (메일 전송 예외)")
        void internalServerErrorWhenMailSenderThrows() throws Exception {
            // given
            String email = uniqueEmail();
            authMailSender.failNextSend();
            String requestBody = emailJson(email);

            // when
            var result = mockMvc.perform(post("/signup/send-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isInternalServerError());
            assertThat(verificationCodeRepository.findCode(email)).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /signup/verify-code")
    class VerifySignupCode {

        @Test
        @DisplayName("성공 (인증 완료)")
        void successWhenCodeMatches() throws Exception {
            // given
            String email = uniqueEmail();
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            String requestBody = verifyCodeJson(email, "ABC123");

            // when
            var result = mockMvc.perform(post("/signup/verify-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            assertThat(verificationCodeRepository.isVerified(email)).isTrue();
        }

        @Test
        @DisplayName("실패 (인증코드 불일치)")
        void badRequestWhenCodeMismatches() throws Exception {
            // given
            String email = uniqueEmail();
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            String requestBody = verifyCodeJson(email, "ZZ9999");

            // when
            var result = mockMvc.perform(post("/signup/verify-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
            assertThat(verificationCodeRepository.isVerified(email)).isFalse();
        }
    }

    @Nested
    @DisplayName("POST /signup")
    class Signup {

        @Test
        @DisplayName("성공 (회원 생성)")
        void successWhenVerificationCompleted() throws Exception {
            // given
            String email = uniqueEmail();
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            verificationCodeRepository.markVerified(email);
            String requestBody = signupJson(email, "a".repeat(128), "ABC123");

            // when
            var result = mockMvc.perform(post("/signup").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isCreated());
            Optional<User> savedUser = userRepository.findByEmailIgnoreCase(email);
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail()).isEqualTo(email);
            assertThat(savedUser.get().getHandle()).isNull();
            assertThat(verificationCodeRepository.findCode(email)).isEmpty();
        }

        @Test
        @DisplayName("실패 (인증 미완료)")
        void badRequestWhenVerificationMissing() throws Exception {
            // given
            String email = uniqueEmail();
            String requestBody = signupJson(email, "a".repeat(128), "ABC123");

            // when
            var result = mockMvc.perform(post("/signup").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
            assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
        }

        @Test
        @DisplayName("실패 (이메일 중복)")
        void conflictWhenEmailDuplicated() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            verificationCodeRepository.markVerified(email);
            String requestBody = signupJson(email, "a".repeat(128), "ABC123");

            // when
            var result = mockMvc.perform(post("/signup").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        @DisplayName("성공 (세션 저장)")
        void successWhenCredentialsMatch() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            saveUser(email, handle, "a".repeat(128));
            String requestBody = loginJson(email, "a".repeat(128));
            String rememberMe = "true";

            // when
            var result = mockMvc.perform(post("/login").with(csrf())
                    .param("remember-me", rememberMe)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.handle").value(handle))
                    .andExpect(cookie().exists("quertimizer-remember-me"));
            User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            assertThat(savedUser.getLastAccessAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 (비밀번호 불일치)")
        void unauthorizedWhenPasswordMismatches() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            String requestBody = loginJson(email, "b".repeat(128));

            // when
            var result = mockMvc.perform(post("/login").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 (로그인 rate limit)")
        void tooManyRequestsWhenLoginFailuresExceedLimit() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            String requestBody = loginJson(email, "b".repeat(128));

            // when & then
            for (int attempt = 0; attempt < 9; attempt++) {
                mockMvc.perform(post("/login").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenPasswordMissing() throws Exception {
            // given
            String requestBody = "{\"email\":\"" + uniqueEmail() + "\"}";

            // when
            var result = mockMvc.perform(post("/login").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /login/social/success")
    class SocialLoginSuccess {

        @Test
        @DisplayName("성공 (소셜 사용자 생성)")
        void successWhenOAuth2AuthenticationExists() throws Exception {
            // given
            String email = uniqueEmail();
            String provider = "github";
            RequestPostProcessor oauth2User = oauth2Login()
                    .clientRegistration(githubClientRegistration())
                    .attributes(attributes -> {
                        attributes.put("id", provider + "-" + UUID.randomUUID());
                        attributes.put("email", email);
                    });

            // when
            var result = mockMvc.perform(get("/login/social/success")
                    .with(oauth2User));

            // then
            result.andExpect(status().isFound())
                    .andExpect(header().string("Location", "http://localhost:5173?socialLoginSuccess=github"));
            assertThat(userRepository.findByEmailIgnoreCase(email)).isPresent();
        }
    }

    @Nested
    @DisplayName("POST /duplicate-check/handle")
    class DuplicateHandle {

        @Test
        @DisplayName("성공 (사용 가능)")
        void successWhenHandleAvailable() throws Exception {
            // given
            String handle = uniqueHandle();
            String requestBody = handleJson(handle);

            // when
            var result = mockMvc.perform(post("/duplicate-check/handle").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 (아이디 중복)")
        void conflictWhenHandleDuplicated() throws Exception {
            // given
            String handle = uniqueHandle();
            saveUser(uniqueEmail(), handle, "a".repeat(128));
            String requestBody = handleJson(handle);

            // when
            var result = mockMvc.perform(post("/duplicate-check/handle").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /duplicate-check/email")
    class DuplicateEmail {

        @Test
        @DisplayName("성공 (사용 가능)")
        void successWhenEmailAvailable() throws Exception {
            // given
            String email = uniqueEmail();
            String requestBody = emailJson(email);

            // when
            var result = mockMvc.perform(post("/duplicate-check/email").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 (이메일 중복)")
        void conflictWhenEmailDuplicated() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            String requestBody = emailJson(email);

            // when
            var result = mockMvc.perform(post("/duplicate-check/email").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /signup/handle")
    class SignupHandle {

        @Test
        @DisplayName("성공 (아이디 설정)")
        void successWhenHandleAvailable() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            saveHandlelessUser(email, "a".repeat(128));
            String requestBody = handleJson(handle);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(email).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/signup/handle").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.handle").value(handle));
            assertThat(userRepository.findByEmailIgnoreCase(email).orElseThrow().getHandle()).isEqualTo(handle);
        }

        @Test
        @DisplayName("실패 (아이디 중복)")
        void conflictWhenHandleDuplicated() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            saveUser(uniqueEmail(), handle, "a".repeat(128));
            saveHandlelessUser(email, "a".repeat(128));
            String requestBody = handleJson(handle);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(email).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/signup/handle").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class Logout {

        @Test
        @DisplayName("성공 (쿠키 삭제)")
        void successWhenAuthenticated() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(email).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/logout").with(csrf()).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(cookie().maxAge("quertimizer-remember-me", 0));
        }
    }

    @Nested
    @DisplayName("POST /session/me")
    class SessionMe {

        @Test
        @DisplayName("성공 (인증 사용자)")
        void successWhenAuthenticated() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            saveUser(email, handle, "a".repeat(128));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(email).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/session/me").with(csrf()).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.handle").value(handle));
        }

        @Test
        @DisplayName("성공 (미인증 사용자)")
        void successWhenUnauthenticated() throws Exception {
            // given

            // when
            var result = mockMvc.perform(post("/session/me").with(csrf()));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false));
        }
    }

    @Nested
    @DisplayName("GET /login/social/failure")
    class SocialLoginFailure {

        @Test
        @DisplayName("성공 (실패 URL 리다이렉트)")
        void successWhenProviderExists() throws Exception {
            // given
            String provider = "github";

            // when
            var result = mockMvc.perform(get("/login/social/failure")
                    .param("provider", provider));

            // then
            result.andExpect(status().isFound())
                    .andExpect(header().string("Location", "http://localhost:5173?socialLoginError=github"));
        }
    }

    @Nested
    @DisplayName("POST /find-password/send-code")
    class FindPasswordSendCode {

        @Test
        @DisplayName("성공 (인증코드 저장)")
        void successWhenEmailExists() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            String requestBody = emailJson(email);

            // when
            var result = mockMvc.perform(post("/find-password/send-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            assertThat(verificationCodeRepository.findCode(email)).isPresent();
            assertThat(authMailSender.sentMails()).extracting(SentMail::to).contains(email);
        }

        @Test
        @DisplayName("성공 (대상 없음)")
        void successWhenEmailMissing() throws Exception {
            // given
            String requestBody = emailJson(uniqueEmail());

            // when
            var result = mockMvc.perform(post("/find-password/send-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /find-password/verify-code")
    class FindPasswordVerifyCode {

        @Test
        @DisplayName("성공 (재설정 인증 완료)")
        void successWhenCodeMatches() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            String requestBody = verifyCodeJson(email, "ABC123");

            // when
            var result = mockMvc.perform(post("/find-password/verify-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            assertThat(verificationCodeRepository.isVerified(email)).isTrue();
        }

        @Test
        @DisplayName("실패 (인증코드 불일치)")
        void badRequestWhenCodeMismatches() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            String requestBody = verifyCodeJson(email, "ZZ9999");

            // when
            var result = mockMvc.perform(post("/find-password/verify-code").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /find-password/reset")
    class FindPasswordReset {

        @Test
        @DisplayName("성공 (비밀번호 변경)")
        void successWhenVerificationCompleted() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            verificationCodeRepository.saveCode(email, "ABC123", LocalDateTime.now().plusMinutes(5));
            verificationCodeRepository.markVerified(email);
            String requestBody = loginJson(email, "b".repeat(128));

            // when
            var result = mockMvc.perform(post("/find-password/reset").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            assertThat(passwordEncoder.matches("b".repeat(128), savedUser.getPassword())).isTrue();
            assertThat(verificationCodeRepository.findCode(email)).isEmpty();
        }

        @Test
        @DisplayName("실패 (인증 미완료)")
        void badRequestWhenVerificationMissing() throws Exception {
            // given
            String email = uniqueEmail();
            saveUser(email, uniqueHandle(), "a".repeat(128));
            String requestBody = loginJson(email, "b".repeat(128));

            // when
            var result = mockMvc.perform(post("/find-password/reset").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    private User saveUser(String email, String handle, String password) {
        return userRepository.save(User.create(handle, passwordEncoder.encode(password), email));
    }

    private User saveHandlelessUser(String email, String password) {
        return userRepository.save(User.create(passwordEncoder.encode(password), email));
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String uniqueHandle() {
        return "h" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static String emailJson(String email) {
        return "{\"email\":\"" + email + "\"}";
    }

    private static String handleJson(String handle) {
        return "{\"handle\":\"" + handle + "\"}";
    }

    private static String verifyCodeJson(String email, String code) {
        return "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}";
    }

    private static String signupJson(String email, String password, String code) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"code\":\"" + code + "\"}";
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
    }

    @TestConfiguration
    static class AuthControllerTestConfiguration {

        @Bean
        @Primary
        FakeAuthMailSender fakeAuthMailSender() {
            return new FakeAuthMailSender();
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return registrationId -> "github".equals(registrationId) ? githubClientRegistration() : null;
        }
    }

    static class FakeAuthMailSender implements AuthMailSender {

        private final List<SentMail> sentMails = new ArrayList<>();
        private boolean failNextSend;

        @Override
        public void sendAuthCodeMail(String to, String subject, String title, String description, String code) {
            if (failNextSend) {
                failNextSend = false;
                throw new RuntimeException("mail failed");
            }

            sentMails.add(new SentMail(to, subject, title, description, code));
        }

        void failNextSend() {
            failNextSend = true;
        }

        void reset() {
            sentMails.clear();
            failNextSend = false;
        }

        List<SentMail> sentMails() {
            return sentMails;
        }
    }

    @Value
    @Accessors(fluent = true)
    static class SentMail {
        String to;
        String subject;
        String title;
        String description;
        String code;
    }
}
