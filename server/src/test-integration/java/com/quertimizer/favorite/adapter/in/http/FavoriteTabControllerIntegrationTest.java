package com.quertimizer.favorite.adapter.in.http;

import com.quertimizer.favorite.application.port.out.FavoriteTabRepositoryPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.domain.model.UserRole;
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
@DisplayName("FavoriteTabController")
class FavoriteTabControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FavoriteTabRepositoryPort favoriteTabRepository;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("GET /profile/me/favorites")
    class GetMyFavoriteTabs {

        @Test
        @DisplayName("성공 (즐겨찾기 조회)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner05", "beginner05@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner05@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/favorites").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.tabs").isArray());
        }

        @Test
        @DisplayName("실패 (미인증)")
        void unauthorizedWhenAuthenticationMissing() throws Exception {
            // given

            // when
            var result = mockMvc.perform(get("/profile/me/favorites"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /profile/me/favorites")
    class UpdateMyFavoriteTabs {

        @Test
        @DisplayName("성공 (즐겨찾기 교체)")
        void successWhenTabsValid() throws Exception {
            // given
            String userEmail = "beginner06@example.com";
            saveUser("beginner06", userEmail, UserRole.USER);
            String requestBody = """
                    {
                      "tabs": [
                        {
                          "label": "문제 목록",
                          "path": "/problems",
                          "snapshot": {"query": "join"}
                        }
                      ]
                    }
                    """;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(userEmail).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(put("/profile/me/favorites").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.tabs[0].label").value("문제 목록"));
            assertThat(favoriteTabRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail)).hasSize(1);
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenLabelBlank() throws Exception {
            // given
            saveUser("beginner06", "beginner06@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner06@example.com").roles(UserRole.USER.name());
            String requestBody = """
                    {
                      "tabs": [
                        {
                          "label": "",
                          "path": "/problems"
                        }
                      ]
                    }
                    """;

            // when
            var result = mockMvc.perform(put("/profile/me/favorites").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    private User saveUser(String handle, String email, UserRole role) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    User user = User.create(handle, passwordEncoder.encode("a".repeat(128)), email);
                    user.changeRole(role);
                    return userRepository.save(user);
                });
    }
}
