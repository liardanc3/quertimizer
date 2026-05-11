package com.quertimizer.problem.adapter.in.web;

import com.quertimizer.global.constant.UserRole;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProblemController")
class ProblemControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /problems")
    class GetProblems {

        @Test
        @DisplayName("성공 (문제 목록)")
        void successWhenProblemsRequested() throws Exception {
            // given
            String page = "1";
            String dbms = "postgresql";

            // when
            var result = mockMvc.perform(get("/problems")
                    .param("page", page)
                    .param("dbms", dbms));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.problems").isArray());
        }

        @Test
        @DisplayName("성공 (검색 필터)")
        void successWhenQueryFilterApplied() throws Exception {
            // given
            String query = "P00001";
            String dbms = "postgresql";

            // when
            var result = mockMvc.perform(get("/problems")
                    .param("query", query)
                    .param("dbms", dbms));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.problems").isArray());
        }
    }

    @Nested
    @DisplayName("GET /problems/{problemId}")
    class GetProblem {

        @Test
        @DisplayName("성공 (문제 상세)")
        void successWhenProblemExists() throws Exception {
            // given
            String problemId = "P00001-00001";

            // when
            var result = mockMvc.perform(get("/problems/{problemId}", problemId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.problemId").value(problemId));
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenProblemMissing() throws Exception {
            // given
            String problemId = "P99999-99999";

            // when
            var result = mockMvc.perform(get("/problems/{problemId}", problemId));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /admin/problem-sets")
    class GetProblemSets {

        @Test
        @DisplayName("성공 (문제셋 목록)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /admin/problem-sets/{problemSetId}")
    class GetProblemSet {

        @Test
        @DisplayName("성공 (문제셋 상세)")
        void successWhenProblemSetExists() throws Exception {
            // given
            String problemSetId = "P00001";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets/{problemSetId}", problemSetId).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.problemSetId").value(problemSetId));
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenProblemSetMissing() throws Exception {
            // given
            String problemSetId = "P99999";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets/{problemSetId}", problemSetId).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /admin/problem-sets/{problemSetId}/problems")
    class GetProblemOptions {

        @Test
        @DisplayName("성공 (문제 옵션)")
        void successWhenProblemSetExists() throws Exception {
            // given
            String problemSetId = "P00001";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets/{problemSetId}/problems", problemSetId).with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            String problemSetId = "P00001";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/problem-sets/{problemSetId}/problems", problemSetId).with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /admin/problems")
    class CreateProblem {

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenTitleBlank() throws Exception {
            // given
            String requestBody = """
                    {
                      "title": "",
                      "description": "설명",
                      "condition": "조건",
                      "output": "출력",
                      "answerSql": "SELECT 1",
                      "dbms": "postgresql",
                      "ddl": "CREATE TABLE t(id int);",
                      "problemDdl": "CREATE TABLE t(id int);",
                      "actualDataSql": "INSERT INTO t VALUES (1);"
                    }
                    """;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/problems").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            String requestBody = "{}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/admin/problems").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /admin/problems/output-preview")
    class PreviewProblem {

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenDdlMissing() throws Exception {
            // given
            String requestBody = """
                    {
                      "dbms": "postgresql",
                      "ddl": "",
                      "actualDataSql": "INSERT INTO t VALUES (1);",
                      "answerSql": "SELECT 1"
                    }
                    """;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/problems/output-preview").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            String requestBody = "{}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/admin/problems/output-preview").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isForbidden());
        }
    }
}
