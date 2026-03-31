package com.quertimizer.endpoint.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.endpoint.api.handler.ApiExceptionHandler;
import com.quertimizer.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("signup")
    class Signup {

        private static final String SIGNUP_URL = "/signup";

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("201 Created 반환")
            void created() throws Exception {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                // when
                ResultActions result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                // then
                result.andExpect(status().isCreated());
                verify(userService).signup(any(SignupReq.class));
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("400 Bad Request (비정상 파라미터)")
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
                verify(userService, never()).signup(any(SignupReq.class));
            }
        }
    }
}
