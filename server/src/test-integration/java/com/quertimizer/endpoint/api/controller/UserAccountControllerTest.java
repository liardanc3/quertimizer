package com.quertimizer.endpoint.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.constant.LoginFailReason;
import com.quertimizer.constant.SignupFailReason;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.endpoint.api.dto.request.DuplicateCheckUserIdReq;
import com.quertimizer.endpoint.api.dto.request.LoginReq;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.entity.User;
import com.quertimizer.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
                String userId = uniqueUserId();
                String email = uniqueEmail();
                SignupReq request = SignupReq.builder()
                        .userId(userId)
                        .password("a".repeat(128))
                        .email(email)
                        .build();

                // when
                MvcResult mvcResult = postSignup(request)
                        .andExpect(status().isCreated())
                        .andReturn();

                // then
                User savedUser = userRepository.findById(userId).orElseThrow();
                assertThat(savedUser.getUserId()).isEqualTo(userId);
                assertThat(savedUser.getEmail()).isEqualTo(email);
                assertThat(savedUser.getPassword()).isEqualTo(Sha512DigestUtils.shaHex(request.getPassword()));
                assertSessionAuthenticated(mvcResult.getRequest().getSession(false), userId);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (userId 중복)")
            void conflictWhenUserIdDuplicated() throws Exception {
                // given
                String userId = uniqueUserId();
                String savedEmail = uniqueEmail();
                String requestEmail = uniqueEmail();
                userRepository.save(User.create(userId, "b".repeat(128), savedEmail));

                SignupReq request = SignupReq.builder()
                        .userId(userId)
                        .password("a".repeat(128))
                        .email(requestEmail)
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_USER_ID));

                User savedUser = userRepository.findById(userId).orElseThrow();
                assertThat(savedUser.getEmail()).isEqualTo(savedEmail);
                assertThat(userRepository.existsByEmail(requestEmail)).isFalse();
            }

            @Test
            @DisplayName("409 Conflict 반환 (email 중복)")
            void conflictWhenEmailDuplicated() throws Exception {
                // given
                String savedUserId = uniqueUserId();
                String requestUserId = uniqueUserId();
                String email = uniqueEmail();
                userRepository.save(User.create(savedUserId, "b".repeat(128), email));

                SignupReq request = SignupReq.builder()
                        .userId(requestUserId)
                        .password("a".repeat(128))
                        .email(email)
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_EMAIL));

                assertThat(userRepository.existsByUserId(requestUserId)).isFalse();
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            @Test
            @DisplayName("동시 요청 (1건 201 Created, 99건 423 Locked or 409 Conflict)")
            void createdAndConflictOrLocked() throws Exception {
                // given
                String lockTestUserId = uniqueUserId();
                SignupReq request = SignupReq.builder()
                        .userId(lockTestUserId)
                        .password("a".repeat(128))
                        .email(uniqueEmail())
                        .build();
                ExecutorService executorService = Executors.newFixedThreadPool(100);
                List<Future<Integer>> futures = new ArrayList<>();

                // when
                for (int i = 0; i < 100; i++) {
                    futures.add(
                            executorService.submit(
                                    () -> postSignup(request)
                                            .andReturn()
                                            .getResponse()
                                            .getStatus()
                            )
                    );
                }

                Map<Integer, Integer> statusAndCountMap = new HashMap<>(Map.of(201, 0, 409, 0, 423, 0));
                for (Future<Integer> future : futures) {
                    statusAndCountMap.merge(future.get(), 1, Integer::sum);
                }
                executorService.shutdown();

                // then
                assertThat(statusAndCountMap.get(201) + statusAndCountMap.get(409) + statusAndCountMap.get(423)).isEqualTo(100);
                assertThat(statusAndCountMap.get(201)).isEqualTo(1);
                assertThat(statusAndCountMap.get(409) + statusAndCountMap.get(423)).isEqualTo(99);
                assertThat(userRepository.existsByUserId(lockTestUserId)).isTrue();
            }
        }

        private ResultActions postSignup(SignupReq request) throws Exception {
            return mockMvc.perform(post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
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
            @DisplayName("200 OK + true (userId 사용 가능)")
            void okWhenUserIdAvailable() throws Exception {
                // given
                DuplicateCheckUserIdReq request = DuplicateCheckUserIdReq.builder()
                        .userId(uniqueUserId())
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(true))
                        .andExpect(jsonPath("$.reason").doesNotExist());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("200 OK + false (userId 중복)")
            void okWhenUserIdDuplicated() throws Exception {
                // given
                String userId = uniqueUserId();
                DuplicateCheckUserIdReq request = DuplicateCheckUserIdReq.builder()
                        .userId(userId)
                        .build();
                userRepository.save(User.create(userId, "b".repeat(128), uniqueEmail()));

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_USER_ID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(false))
                        .andExpect(jsonPath("$.reason").value(SignupFailReason.DUPLICATED_USER_ID));
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
            @DisplayName("200 OK + true (email 사용 가능)")
            void okWhenEmailAvailable() throws Exception {
                // given
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email(uniqueEmail())
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(true))
                        .andExpect(jsonPath("$.reason").doesNotExist());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("200 OK + false (email 중복)")
            void okWhenEmailDuplicated() throws Exception {
                // given
                String email = uniqueEmail();
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email(email)
                        .build();
                userRepository.save(User.create(uniqueUserId(), "b".repeat(128), email));

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.available").value(false))
                        .andExpect(jsonPath("$.reason").value(SignupFailReason.DUPLICATED_EMAIL));
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
                String userId = uniqueUserId();
                String rawPassword = "a".repeat(128);
                userRepository.save(User.create(userId, Sha512DigestUtils.shaHex(rawPassword), uniqueEmail()));

                LoginReq request = LoginReq.builder()
                        .userId(userId)
                        .password(rawPassword)
                        .build();

                // when
                MvcResult mvcResult = postLogin(request)
                        .andExpect(status().isOk())
                        .andReturn();

                // then
                assertSessionAuthenticated(mvcResult.getRequest().getSession(false), userId);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("401 Unauthorized 반환 (아이디 또는 비밀번호 불일치)")
            void unauthorizedWhenCredentialsMismatch() throws Exception {
                // given
                String userId = uniqueUserId();
                userRepository.save(User.create(userId, Sha512DigestUtils.shaHex("b".repeat(128)), uniqueEmail()));

                LoginReq request = LoginReq.builder()
                        .userId(userId)
                        .password("a".repeat(128))
                        .build();

                // when
                ResultActions result = postLogin(request);

                // then
                result.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.reasons[0]").value(LoginFailReason.INVALID_USER_ID_OR_PASSWORD));
            }
        }

        private ResultActions postLogin(LoginReq request) throws Exception {
            return mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }

    private void assertSessionAuthenticated(HttpSession session, String userId) {
        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getName()).isEqualTo(userId);
    }

    private String uniqueUserId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
