package com.quertimizer.user.adapter.in.http;

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
@DisplayName("UserProfileController")
class UserProfileControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("GET /profile/me")
    class GetMyProfile {

        @Test
        @DisplayName("성공 (내 프로필)")
        void successWhenAuthenticated() throws Exception {
            // given
            String handle = "beginner01";
            saveUser(handle, "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.handle").value(handle));
        }

        @Test
        @DisplayName("실패 (미인증)")
        void unauthorizedWhenAuthenticationMissing() throws Exception {
            // given

            // when
            var result = mockMvc.perform(get("/profile/me"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/solved-problems")
    class GetMySolvedProblems {

        @Test
        @DisplayName("성공 (내 해결 문제)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/solved-problems").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.solvedProblemIds").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/solved-records")
    class GetMySolvedRecords {

        @Test
        @DisplayName("성공 (내 해결 기록)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/solved-records").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.solvedRecords").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/submission-summary")
    class GetMySubmissionSummary {

        @Test
        @DisplayName("성공 (내 제출 요약)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/submission-summary").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptedProblemIds").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/community/posts")
    class GetMyCommunityPosts {

        @Test
        @DisplayName("성공 (내 게시글)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/community/posts").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/community/liked-posts")
    class GetMyLikedPosts {

        @Test
        @DisplayName("성공 (내 좋아요 게시글)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/community/liked-posts").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/community/comments")
    class GetMyCommunityComments {

        @Test
        @DisplayName("성공 (내 댓글)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/community/comments").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/community/liked-comments")
    class GetMyLikedComments {

        @Test
        @DisplayName("성공 (내 좋아요 댓글)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/community/liked-comments").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profile/me/community/activities")
    class GetMyCommunityActivities {

        @Test
        @DisplayName("성공 (내 활동)")
        void successWhenAuthenticated() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/profile/me/community/activities").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.activities").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /profile/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("성공 (프로필 수정)")
        void successWhenRequestValid() throws Exception {
            // given
            String email = uniqueEmail();
            String handle = uniqueHandle();
            userRepository.save(User.create(handle, passwordEncoder.encode("a".repeat(128)), email));
            String requestBody = """
                    {
                      "bio": "수정 소개",
                      "profileImageUrl": "",
                      "backgroundImageUrl": "",
                      "links": [
                        {
                          "type": "github",
                          "value": "https://github.com/test"
                        }
                      ],
                      "defaultDbms": "POSTGRESQL",
                      "sqlPublic": true,
                      "executionPercentilePublic": true,
                      "solvedRecordsPublic": true,
                      "solvedProblemCountPublic": true,
                      "communityActivityPublic": true
                    }
                    """;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user(email).roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(put("/profile/me").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.bio").value("수정 소개"));
            assertThat(userRepository.findByHandle(handle).orElseThrow().getBio()).isEqualTo("수정 소개");
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenDefaultDbmsMissing() throws Exception {
            // given
            saveUser("beginner01", "beginner01@example.com", UserRole.USER);
            String requestBody = "{\"links\":[]}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(put("/profile/me").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}")
    class GetProfile {

        @Test
        @DisplayName("성공 (공개 프로필)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.handle").value(handle));
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenHandleMissing() throws Exception {
            // given
            String handle = uniqueHandle();

            // when
            var result = mockMvc.perform(get("/profiles/{handle}", handle));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/solved-problems")
    class GetSolvedProblems {

        @Test
        @DisplayName("성공 (공개 해결 문제)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/solved-problems", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.solvedProblemIds").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/solved-records")
    class GetSolvedRecords {

        @Test
        @DisplayName("성공 (공개 해결 기록)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/solved-records", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.solvedRecords").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/submission-summary")
    class GetSubmissionSummary {

        @Test
        @DisplayName("성공 (공개 제출 요약)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/submission-summary", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptedProblemIds").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/community/posts")
    class GetCommunityPosts {

        @Test
        @DisplayName("성공 (공개 게시글)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/community/posts", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/community/liked-posts")
    class GetLikedPosts {

        @Test
        @DisplayName("성공 (공개 좋아요 게시글)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/community/liked-posts", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/community/comments")
    class GetCommunityComments {

        @Test
        @DisplayName("성공 (공개 댓글)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/community/comments", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/community/liked-comments")
    class GetLikedComments {

        @Test
        @DisplayName("성공 (공개 좋아요 댓글)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/community/liked-comments", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray());
        }
    }

    @Nested
    @DisplayName("GET /profiles/{handle}/community/activities")
    class GetCommunityActivities {

        @Test
        @DisplayName("성공 (공개 활동)")
        void successWhenHandleExists() throws Exception {
            // given
            String handle = "liardanc3";
            saveUser(handle, "liardanc3@example.com", UserRole.USER);

            // when
            var result = mockMvc.perform(get("/profiles/{handle}/community/activities", handle));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.activities").isArray());
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
