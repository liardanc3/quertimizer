package com.quertimizer.ranking.adapter.in.web;

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
@DisplayName("RankController")
class RankControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /ranks")
    class GetRanks {

        @Test
        @DisplayName("성공 (랭킹 조회)")
        void successWhenRankPageRequested() throws Exception {
            // given
            String page = "1";

            // when
            var result = mockMvc.perform(get("/ranks")
                    .param("page", page));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.ranks").isArray());
        }

        @Test
        @DisplayName("성공 (페이지 보정)")
        void successWhenPageBelowMinimum() throws Exception {
            // given
            String page = "0";

            // when
            var result = mockMvc.perform(get("/ranks")
                    .param("page", page));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPage").value(1));
        }
    }
}
