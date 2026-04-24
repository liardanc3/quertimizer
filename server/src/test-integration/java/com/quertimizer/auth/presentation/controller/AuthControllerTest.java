package com.quertimizer.auth.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.auth.domain.model.LoginFailReason;
import com.quertimizer.auth.domain.model.SignupFailReason;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckEmailReq;
import com.quertimizer.auth.presentation.dto.request.DuplicateCheckHandleReq;
import com.quertimizer.auth.presentation.dto.request.LoginReq;
import com.quertimizer.auth.presentation.dto.request.SignupReq;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.infrastructure.repository.UserJpaRepository;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

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
                String handle = uniqueHandle();
                String email = uniqueEmail();
                SignupReq request = SignupReq.builder()
                        .handle(handle)
                        .password("a".repeat(128))
                        .email(email)
                        .build();

                // when
                MvcResult mvcResult = postSignup(request)
                        .andExpect(status().isCreated())
                        .andReturn();

                // then
                User savedUser = userRepository.findById(handle).orElseThrow();
                assertThat(savedUser.getHandle()).isEqualTo(handle);
                assertThat(savedUser.getEmail()).isEqualTo(email);
                assertThat(savedUser.getPassword()).isEqualTo(Sha512DigestUtils.shaHex(request.getPassword()));
                assertSessionAuthenticated(mvcResult.getRequest().getSession(false), handle);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (handle 중복)")
            void conflictWhenHandleDuplicated() throws Exception {
                // given
                String handle = uniqueHandle();
                String savedEmail = uniqueEmail();
                String requestEmail = uniqueEmail();
                userRepository.save(User.create(handle, "b".repeat(128), savedEmail));

                SignupReq request = SignupReq.builder()
                        .handle(handle)
                        .password("a".repeat(128))
                        .email(requestEmail)
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_HANDLE.getMessage()));

                User savedUser = userRepository.findById(handle).orElseThrow();
                assertThat(savedUser.getEmail()).isEqualTo(savedEmail);
                assertThat(userRepository.existsByEmail(requestEmail)).isFalse();
            }

            @Test
            @DisplayName("409 Conflict 반환 (email 중복)")
            void conflictWhenEmailDuplicated() throws Exception {
                // given
                String savedHandle = uniqueHandle();
                String requestHandle = uniqueHandle();
                String email = uniqueEmail();
                userRepository.save(User.create(savedHandle, "b".repeat(128), email));

                SignupReq request = SignupReq.builder()
                        .handle(requestHandle)
                        .password("a".repeat(128))
                        .email(email)
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_EMAIL.getMessage()));

                assertThat(userRepository.existsByHandle(requestHandle)).isFalse();
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            @Test
            @DisplayName("동시 요청 (1건 201 Created, 99건 423 Locked or 409 Conflict)")
            void createdAndConflictOrLocked() throws Exception {
                // given
                String lockTestHandle = uniqueHandle();
                SignupReq request = SignupReq.builder()
                        .handle(lockTestHandle)
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
                assertThat(userRepository.existsByHandle(lockTestHandle)).isTrue();
            }
        }

        private ResultActions postSignup(SignupReq request) throws Exception {
            // 실제 controller 진입점과 동일한 JSON 요청 형태를 helper로 고정한다.
            return mockMvc.perform(post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
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
            @DisplayName("200 OK 반환 (handle 사용 가능)")
            void okWhenHandleAvailable() throws Exception {
                // given
                DuplicateCheckHandleReq request = DuplicateCheckHandleReq.builder()
                        .handle(uniqueHandle())
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_HANDLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (handle 중복)")
            void conflictWhenHandleDuplicated() throws Exception {
                // given
                String handle = uniqueHandle();
                DuplicateCheckHandleReq request = DuplicateCheckHandleReq.builder()
                        .handle(handle)
                        .build();
                userRepository.save(User.create(handle, "b".repeat(128), uniqueEmail()));

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_HANDLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_HANDLE.getMessage()));
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
            @DisplayName("200 OK 반환 (email 사용 가능)")
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
                result.andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict 반환 (email 중복)")
            void conflictWhenEmailDuplicated() throws Exception {
                // given
                String email = uniqueEmail();
                DuplicateCheckEmailReq request = DuplicateCheckEmailReq.builder()
                        .email(email)
                        .build();
                userRepository.save(User.create(uniqueHandle(), "b".repeat(128), email));

                // when
                ResultActions result = mockMvc.perform(post(DUPLICATE_CHECK_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_EMAIL.getMessage()));
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
                String handle = uniqueHandle();
                String rawPassword = "a".repeat(128);
                userRepository.save(User.create(handle, Sha512DigestUtils.shaHex(rawPassword), uniqueEmail()));

                LoginReq request = LoginReq.builder()
                        .handle(handle)
                        .password(rawPassword)
                        .build();

                // when
                MvcResult mvcResult = postLogin(request)
                        .andExpect(status().isOk())
                        .andReturn();

                // then
                assertSessionAuthenticated(mvcResult.getRequest().getSession(false), handle);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("401 Unauthorized 반환 (이메일 또는 비밀번호 불일치)")
            void unauthorizedWhenCredentialsMismatch() throws Exception {
                // given
                String handle = uniqueHandle();
                userRepository.save(User.create(handle, Sha512DigestUtils.shaHex("b".repeat(128)), uniqueEmail()));

                LoginReq request = LoginReq.builder()
                        .handle(handle)
                        .password("a".repeat(128))
                        .build();

                // when
                ResultActions result = postLogin(request);

                // then
                result.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.reasons[0]").value(LoginFailReason.INVALID_EMAIL_OR_PASSWORD.getMessage()));
            }
        }

        private ResultActions postLogin(LoginReq request) throws Exception {
            // 실제 controller 진입점과 동일한 JSON 요청 형태를 helper로 고정한다.
            return mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }

    private void assertSessionAuthenticated(HttpSession session, String handle) {
        // 통합 테스트에서는 HTTP 응답뿐 아니라 실제 HttpSession 저장 결과까지 확인한다.
        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getName()).isEqualTo(handle);
    }

    private String uniqueHandle() {
        // 병렬 가입 테스트에서도 충돌하지 않도록 랜덤 handle를 사용한다.
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueEmail() {
        // 병렬 가입 테스트에서도 충돌하지 않도록 랜덤 이메일을 사용한다.
        return UUID.randomUUID() + "@example.com";
    }
}
