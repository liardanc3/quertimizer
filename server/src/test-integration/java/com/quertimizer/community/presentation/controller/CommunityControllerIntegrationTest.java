package com.quertimizer.community.presentation.controller;

import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityCommentLikeId;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLikeId;
import com.quertimizer.global.constant.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CommunityController")
class CommunityControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CommunityPostRepository communityPostRepository;
    @Autowired private CommunityCommentRepository communityCommentRepository;
    @Autowired private CommunityPostLikeRepository communityPostLikeRepository;
    @Autowired private CommunityCommentLikeRepository communityCommentLikeRepository;

    @Nested
    @DisplayName("GET /community/posts")
    class GetPosts {

        @Test
        @DisplayName("성공 (게시글 목록)")
        void successWhenPostsRequested() throws Exception {
            // given
            String page = "1";

            // when
            var result = mockMvc.perform(get("/community/posts").param("page", page));

            // then
            result.andExpect(status().isOk())
                  .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /community/posts/{postId}")
    class GetPostDetail {

        @Test
        @DisplayName("성공 (게시글 상세)")
        void successWhenPostExists() throws Exception {
            // given
            long postId = 1L;
            String formattedPostId = "000000001";

            // when
            var result = mockMvc.perform(get("/community/posts/{postId}", postId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(formattedPostId));
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenPostMissing() throws Exception {
            // given
            long postId = 99999999L;

            // when
            var result = mockMvc.perform(get("/community/posts/{postId}", postId));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /community/posts")
    class CreatePost {

        @Test
        @DisplayName("성공 (게시글 생성)")
        void successWhenRequestValid() throws Exception {
            // given
            String handle = "beginner07";
            String requestBody = communityPostJson("새 게시글", "새 게시글 요약");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner07@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts")
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
            assertThat(communityPostRepository.findAllByHandleOrderByCreatedAtDesc(handle))
                    .extracting(CommunityPost::getTitle)
                    .contains("새 게시글");
        }

        @Test
        @DisplayName("실패 (미인증)")
        void unauthorizedWhenAuthenticationMissing() throws Exception {
            // given
            String requestBody = communityPostJson("새 게시글", "새 게시글 요약");

            // when
            var result = mockMvc.perform(post("/community/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenTitleBlank() throws Exception {
            // given
            String requestBody = communityPostJson("", "새 게시글 요약");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner07@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts")
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /community/images")
    class UploadImage {

        @Test
        @DisplayName("성공 (이미지 업로드)")
        void successWhenImageValid() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner08@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(multipart("/community/images")
                    .file(file)
                    .with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.imageId").exists())
                    .andExpect(jsonPath("$.imageUrl").exists());
        }

        @Test
        @DisplayName("실패 (파일 형식)")
        void badRequestWhenFileTypeInvalid() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "text".getBytes());
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner08@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(multipart("/community/images")
                    .file(file)
                    .with(user));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /community/images/{imageId}")
    class GetImage {

        @Test
        @DisplayName("성공 (이미지 조회)")
        void successWhenImageExists() throws Exception {
            // given
            String imageId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png";
            Path imagePath = Path.of("build/test-community-images").toAbsolutePath().normalize().resolve(imageId);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, new byte[]{1, 2, 3});

            // when
            var result = mockMvc.perform(get("/community/images/{imageId}", imageId));

            // then
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenImageMissing() throws Exception {
            // given
            String imageId = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png";

            // when
            var result = mockMvc.perform(get("/community/images/{imageId}", imageId));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /community/posts/{postId}")
    class UpdatePost {

        @Test
        @DisplayName("성공 (게시글 수정)")
        void successWhenAuthorMatches() throws Exception {
            // given
            long postId = 909001L;
            String handle = "beginner09";
            CommunityPost post = communityPostRepository.save(CommunityPost.create(
                    postId, handle, "수정 전", "{}", "수정 전", "", "discussion", LocalDateTime.now()
            ));
            String requestBody = communityPostJson("수정 후", "수정 후 요약");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner09@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(put("/community/posts/{postId}", postId)
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isNoContent());
            assertThat(communityPostRepository.findById(post.getPostId()).orElseThrow().getTitle()).isEqualTo("수정 후");
        }

        @Test
        @DisplayName("실패 (소유자 아님)")
        void forbiddenWhenAuthorDifferent() throws Exception {
            // given
            long postId = 909002L;
            String handle = "beginner09";
            CommunityPost post = communityPostRepository.save(CommunityPost.create(
                    postId, handle, "수정 전", "{}", "수정 전", "", "discussion", LocalDateTime.now()
            ));
            long requestedPostId = post.getPostId();
            String requestBody = communityPostJson("수정 후", "수정 후 요약");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner10@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(put("/community/posts/{postId}", requestedPostId)
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /community/posts/{postId}")
    class DeletePost {

        @Test
        @DisplayName("성공 (게시글 삭제)")
        void successWhenAuthorMatches() throws Exception {
            // given
            long postId = 909003L;
            String handle = "advanced01";
            CommunityPost post = communityPostRepository.save(CommunityPost.create(
                    postId, handle, "삭제 대상", "{}", "삭제 대상", "", "discussion", LocalDateTime.now()
            ));
            long requestedPostId = post.getPostId();
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("advanced01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(delete("/community/posts/{postId}", requestedPostId).with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(communityPostRepository.findById(post.getPostId())).isEmpty();
        }

        @Test
        @DisplayName("실패 (소유자 아님)")
        void forbiddenWhenAuthorDifferent() throws Exception {
            // given
            long postId = 909004L;
            String handle = "advanced01";
            CommunityPost post = communityPostRepository.save(CommunityPost.create(
                    postId, handle, "삭제 대상", "{}", "삭제 대상", "", "discussion", LocalDateTime.now()
            ));
            long requestedPostId = post.getPostId();
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("advanced02@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(delete("/community/posts/{postId}", requestedPostId).with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /community/posts/{postId}/likes")
    class TogglePostLike {

        @Test
        @DisplayName("성공 (게시글 좋아요)")
        void successWhenPostExists() throws Exception {
            // given
            long postId = 1L;
            String handle = "beginner05";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner05@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts/{postId}/likes", postId).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true));
            assertThat(communityPostLikeRepository.existsById(new CommunityPostLikeId(postId, handle))).isTrue();
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenPostMissing() throws Exception {
            // given
            long postId = 99999999L;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner05@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts/{postId}/likes", postId).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /community/posts/{postId}/comments")
    class AddComment {

        @Test
        @DisplayName("성공 (댓글 생성)")
        void successWhenPostExists() throws Exception {
            // given
            long postId = 1L;
            String requestBody = "{\"content\":\"새 댓글\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner06@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts/{postId}/comments", postId)
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("새 댓글"));
            assertThat(communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId))
                    .extracting(CommunityComment::getContent)
                    .contains("새 댓글");
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenContentBlank() throws Exception {
            // given
            long postId = 1L;
            String requestBody = "{\"content\":\"\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner06@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/posts/{postId}/comments", postId)
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /community/comments/{commentId}/likes")
    class ToggleCommentLike {

        @Test
        @DisplayName("성공 (댓글 좋아요)")
        void successWhenCommentExists() throws Exception {
            // given
            long postId = 1L;
            String handle = "beginner07";
            Long commentId = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId).get(0).getCommentId();
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner07@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/comments/{commentId}/likes", commentId).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true));
            assertThat(communityCommentLikeRepository.existsById(new CommunityCommentLikeId(commentId, handle))).isTrue();
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenCommentMissing() throws Exception {
            // given
            long commentId = 99999999L;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner07@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/community/comments/{commentId}/likes", commentId).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /community/tags/suggestions")
    class GetTagSuggestions {

        @Test
        @DisplayName("성공 (태그 자동완성)")
        void successWhenQueryMatches() throws Exception {
            // given
            String query = "sql";

            // when
            var result = mockMvc.perform(get("/community/tags/suggestions")
                    .param("query", query));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    private static String communityPostJson(String title, String summary) {
        return """
                {
                  "title": "%s",
                  "contentJson": "{}",
                  "plainTextSummary": "%s",
                  "imageIds": [],
                  "tags": ["integration"],
                  "category": "discussion"
                }
                """.formatted(title, summary);
    }
}
