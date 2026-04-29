package com.quertimizer.user.presentation.controller;

import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserAnomalyController")
class UserAnomalyControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BlockedIpRepository blockedIpRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("GET /admin/anomaly-accounts/trends")
    class GetAnomalyTrends {

        @Test
        @DisplayName("성공 (이상 추세 조회)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            String range = "10m";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/anomaly-accounts/trends")
                    .with(user)
                    .param("range", range));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/anomaly-accounts/trends").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /admin/anomaly-accounts/blocked-users")
    class GetBlockedUsers {

        @Test
        @DisplayName("성공 (차단 사용자 조회)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/anomaly-accounts/blocked-users").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /admin/anomaly-accounts/blocked-ips")
    class GetBlockedIps {

        @Test
        @DisplayName("성공 (차단 IP 조회)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/anomaly-accounts/blocked-ips").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    @DisplayName("POST /admin/anomaly-accounts/users/{handle}/block")
    class BlockUser {

        @Test
        @DisplayName("성공 (사용자 차단)")
        void successWhenUserExists() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            User targetUser = User.create(handle, passwordEncoder.encode("a".repeat(128)), email);
            targetUser.updateLastAccess("172.16.0.10", LocalDateTime.now());
            userRepository.save(targetUser);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/anomaly-accounts/users/{handle}/block", handle).with(csrf()).with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(userRepository.findByHandle(handle).orElseThrow().isBlocked()).isTrue();
            assertThat(blockedIpRepository.findById("172.16.0.10")).isPresent();
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenHandleMissing() throws Exception {
            // given
            String handle = uniqueHandle();
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/anomaly-accounts/users/{handle}/block", handle).with(csrf()).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/anomaly-accounts/users/{handle}/block")
    class UnblockUser {

        @Test
        @DisplayName("성공 (사용자 차단 해제)")
        void successWhenUserBlocked() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            User targetUser = User.create(handle, passwordEncoder.encode("a".repeat(128)), email);
            targetUser.block();
            userRepository.save(targetUser);
            blockedIpRepository.save(BlockedIp.create("172.16.0.11", handle));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(delete("/admin/anomaly-accounts/users/{handle}/block", handle).with(csrf()).with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(userRepository.findByHandle(handle).orElseThrow().isBlocked()).isFalse();
            assertThat(blockedIpRepository.findById("172.16.0.11")).isEmpty();
        }
    }

    @Nested
    @DisplayName("DELETE /admin/anomaly-accounts/ips/{ipAddress}/block")
    class UnblockIp {

        @Test
        @DisplayName("성공 (IP 차단 해제)")
        void successWhenIpBlocked() throws Exception {
            // given
            String ipAddress = "172.16.0.12";
            blockedIpRepository.save(BlockedIp.create(ipAddress, null));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(delete("/admin/anomaly-accounts/ips/{ipAddress}/block", ipAddress).with(csrf()).with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(blockedIpRepository.findById(ipAddress)).isEmpty();
        }
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String uniqueHandle() {
        return "h" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
