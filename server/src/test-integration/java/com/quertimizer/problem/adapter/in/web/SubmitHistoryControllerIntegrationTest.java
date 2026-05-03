package com.quertimizer.problem.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SubmitHistoryController")
class SubmitHistoryControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /submit-histories")
    class SubmitHistoryService {

        @Test
        @DisplayName("성공 (제출 이력 조회)")
        void successWhenSubmitHistoriesRequested() throws Exception {
            // given
            String page = "1";

            // when
            var result = mockMvc.perform(get("/submit-histories")
                    .param("page", page));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.histories").isArray())
                    .andExpect(jsonPath("$.problemIds").isArray());
        }

        @Test
        @DisplayName("성공 (문제 필터)")
        void successWhenProblemFilterApplied() throws Exception {
            // given
            String problemId = "P00001-00001";

            // when
            var result = mockMvc.perform(get("/submit-histories")
                    .param("problemId", problemId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.histories").isArray());
        }
    }
}
