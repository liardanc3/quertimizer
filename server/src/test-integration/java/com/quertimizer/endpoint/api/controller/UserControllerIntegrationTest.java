package com.quertimizer.endpoint.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.constant.SignupFailReason;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.entity.User;
import com.quertimizer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.datasource.url=jdbc:h2:mem:signup-integration-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        private static final String SIGNUP_URL = "/signup";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("201 Created")
            void created() throws Exception {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isCreated());

                User savedUser = userRepository.findById(request.getUserId()).orElseThrow();

                assertThat(savedUser.getUserId()).isEqualTo(request.getUserId());
                assertThat(savedUser.getEmail()).isEqualTo(request.getEmail());
                assertThat(savedUser.getPassword()).isEqualTo(Sha512DigestUtils.shaHex(request.getPassword()));
                assertThat(userRepository.count()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("409 Conflict (userId 중복)")
            void conflictWhenUserIdDuplicated() throws Exception {
                // given
                userRepository.save(User.create("tester", "b".repeat(128), "saved@example.com"));

                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_USER_ID));

                User savedUser = userRepository.findById("tester").orElseThrow();
                assertThat(savedUser.getEmail()).isEqualTo("saved@example.com");
                assertThat(userRepository.count()).isEqualTo(1);
                assertThat(userRepository.existsByEmail("tester@example.com")).isFalse();
            }

            @Test
            @DisplayName("409 Conflict (email 중복)")
            void conflictWhenEmailDuplicated() throws Exception {
                // given
                userRepository.save(User.create("saved-user", "b".repeat(128), "tester@example.com"));

                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                // when
                ResultActions result = postSignup(request);

                // then
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.reasons[0]").value(SignupFailReason.DUPLICATED_EMAIL));

                assertThat(userRepository.existsByUserId("tester")).isFalse();
                assertThat(userRepository.count()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("특수")
        class Special {

            private static final String LOCK_TEST_USER_ID = "lock-user";

            @Test
            @DisplayName("동시 요청 (1건 201 Created, 99건 423 Locked or 409 Conflict)")
            void createdAndConflictOrLocked() throws Exception {
                // given
                SignupReq request = SignupReq.builder()
                        .userId(LOCK_TEST_USER_ID)
                        .password("a".repeat(128))
                        .email("lock-user@example.com")
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
                    statusAndCountMap.compute(future.get(), (k, v) -> v == null ? 1 : v + 1);
                }
                executorService.shutdown();

                // then
                assertThat(statusAndCountMap.get(201) + statusAndCountMap.get(409) + statusAndCountMap.get(423)).isEqualTo(100);
                assertThat(statusAndCountMap.get(201)).isEqualTo(1);
                assertThat(statusAndCountMap.get(409) + statusAndCountMap.get(423)).isEqualTo(99);
                assertThat(userRepository.existsByUserId(LOCK_TEST_USER_ID)).isTrue();
            }

            private int requestSignupStatus(SignupReq request) throws Exception {
                return postSignup(request)
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }
        }

        private ResultActions postSignup(SignupReq request) throws Exception {
            return mockMvc.perform(post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }
}
