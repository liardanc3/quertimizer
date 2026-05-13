package com.quertimizer.auth.adapter.in.http;

import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthManageController")
class AuthManageControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("GET /admin/auth-manage")
    class GetAuthManage {

        @Test
        @DisplayName("성공 (관리자 조회)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/auth-manage").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/auth-manage").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /admin/auth-manage/users/{handle}/role")
    class UpdateUserRole {

        @Test
        @DisplayName("성공 (역할 변경)")
        void successWhenRoleValid() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            String email = uniqueEmail();
            String handle = uniqueHandle();
            userRepository.save(User.create(handle, passwordEncoder.encode("a".repeat(128)), email));
            String requestBody = "{\"role\":\"admin\",\"confirmationText\":\"ROLE_CHANGE_CONFIRMED\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/auth-manage/users/{handle}/role", handle).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk());
            assertThat(userRepository.findByHandle(handle).orElseThrow().getResolvedRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenRoleBlank() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            String handle = "beginner01";
            String requestBody = "{\"role\":\"\",\"confirmationText\":\"ROLE_CHANGE_CONFIRMED\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/auth-manage/users/{handle}/role", handle).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (민감 작업 확인 누락)")
        void badRequestWhenSensitiveConfirmationInvalid() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            String handle = "beginner01";
            String requestBody = "{\"role\":\"admin\",\"confirmationText\":\"WRONG\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/auth-manage/users/{handle}/role", handle).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (자기 ADMIN 권한 제거)")
        void badRequestWhenSelfAdminRemovalRequested() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            String handle = "admin";
            String requestBody = "{\"role\":\"user\",\"confirmationText\":\"ROLE_CHANGE_CONFIRMED\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/auth-manage/users/{handle}/role", handle).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
            assertThat(userRepository.findByHandle(handle).orElseThrow().getResolvedRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenHandleMissing() throws Exception {
            // given
            saveUser("admin", "admin@example.com", UserRole.ADMIN);
            String handle = uniqueHandle();
            String requestBody = "{\"role\":\"user\",\"confirmationText\":\"ROLE_CHANGE_CONFIRMED\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/auth-manage/users/{handle}/role", handle).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private User saveUser(String handle, String email, UserRole role) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    User user = User.create(handle, passwordEncoder.encode("a".repeat(128)), email);
                    user.changeRole(role);
                    return userRepository.save(user);
                });
    }

    private static String uniqueHandle() {
        return "h" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
