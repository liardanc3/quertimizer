package com.quertimizer.ui.adapter.in.http;

import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UiTextController")
class UiTextControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UiTextRepositoryPort uiTextRepository;

    @Nested
    @DisplayName("GET /ui-texts")
    class GetUiTexts {

        @Test
        @DisplayName("성공 (언어별 조회)")
        void successWhenLanguageExists() throws Exception {
            // given
            String language = "kr";

            // when
            var result = mockMvc.perform(get("/ui-texts")
                    .param("language", language));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /ui-texts/{key}")
    class GetUiText {

        @Test
        @DisplayName("성공 (단건 조회)")
        void successWhenKeyExists() throws Exception {
            // given
            String key = "COMMON_CONFIRM_BUTTON";
            String language = "kr";

            // when
            var result = mockMvc.perform(get("/ui-texts/{key}", key)
                    .param("language", language));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.key").value("COMMON_CONFIRM_BUTTON"));
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenKeyMissing() throws Exception {
            // given
            String key = "UNKNOWN_TEST_KEY";
            String language = "kr";

            // when
            var result = mockMvc.perform(get("/ui-texts/{key}", key)
                    .param("language", language));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /admin/ui-texts")
    class GetAdminUiTexts {

        @Test
        @DisplayName("성공 (관리자 목록)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/ui-texts").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.uiTexts").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/ui-texts").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /admin/ui-texts")
    class CreateUiText {

        @Test
        @DisplayName("성공 (UI 텍스트 생성)")
        void successWhenRequestValid() throws Exception {
            // given
            String key = uniqueKey();
            String language = "kr";
            String requestBody = uiTextJson(key, "생성 값", language, "생성 설명");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/ui-texts").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.key").value(key));
            assertThat(uiTextRepository.findById(UiTextId.create(key, language))).isPresent();
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenKeyBlank() throws Exception {
            // given
            String requestBody = uiTextJson("", "생성 값", "kr", "생성 설명");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/ui-texts").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /admin/ui-texts/{key}/{language}")
    class UpdateUiText {

        @Test
        @DisplayName("성공 (UI 텍스트 수정)")
        void successWhenTextExists() throws Exception {
            // given
            String key = uniqueKey();
            String language = "kr";
            uiTextRepository.save(com.quertimizer.ui.domain.entity.UiText.create(key, "기존 값", language, "기존 설명"));
            String requestBody = uiTextJson(key, "수정 값", language, "수정 설명");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/ui-texts/{key}/{language}", key, language).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.value").value("수정 값"));
            assertThat(uiTextRepository.findById(UiTextId.create(key, language)).orElseThrow().getValue()).isEqualTo("수정 값");
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenTextMissing() throws Exception {
            // given
            String key = uniqueKey();
            String language = "kr";
            String requestBody = uiTextJson(uniqueKey(), "수정 값", language, "수정 설명");
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/ui-texts/{key}/{language}", key, language).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/ui-texts/{key}/{language}")
    class DeleteUiText {

        @Test
        @DisplayName("성공 (UI 텍스트 삭제)")
        void successWhenTextExists() throws Exception {
            // given
            String key = uniqueKey();
            String language = "kr";
            uiTextRepository.save(com.quertimizer.ui.domain.entity.UiText.create(key, "삭제 값", language, "삭제 설명"));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(delete("/admin/ui-texts/{key}/{language}", key, language).with(csrf()).with(user));

            // then
            result.andExpect(status().isOk());
            assertThat(uiTextRepository.findById(UiTextId.create(key, language))).isEmpty();
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenTextMissing() throws Exception {
            // given
            String key = uniqueKey();
            String language = "kr";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(delete("/admin/ui-texts/{key}/{language}", key, language).with(csrf()).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    private static String uniqueKey() {
        return "TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private static String uiTextJson(String key, String value, String language, String description) {
        return """
                {
                  "key": "%s",
                  "value": "%s",
                  "language": "%s",
                  "description": "%s"
                }
                """.formatted(key, value, language, description);
    }
}
