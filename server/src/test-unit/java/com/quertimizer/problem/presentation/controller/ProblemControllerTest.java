package com.quertimizer.problem.presentation.controller;

import com.quertimizer.problem.presentation.dto.response.ProblemListItemRes;
import com.quertimizer.problem.presentation.dto.response.ProblemPageRes;
import com.quertimizer.problem.presentation.dto.response.ProblemSubmittedHistoryRes;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.problem.application.service.ProblemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemService problemService;

    @MockitoBean
    private LogFormatter logFormatter;

    @Test
    @DisplayName("GET /problems : 200 OK + 페이지 응답 반환")
    void okAndReturnProblemPage() throws Exception {
        // given
        when(problemService.getProblems(eq(1), isNull(), eq("all"), isNull(), eq("desc"), eq("none"), isNull(), isNull()))
                .thenReturn(new ProblemPageRes(
                        1,
                        20,
                        1,
                        1,
                        0.0,
                        0.0,
                        List.of(
                                new ProblemListItemRes(
                                        "00001-00001",
                                        "3월 고객별 주문 건수와 총 주문 금액 조회",
                                        "문제 설명",
                                        List.of(new ProblemSubmittedHistoryRes("postgresql", "problemuser001", 3L, 95L))
                                )
                        )
                ));

        // when & then
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.spreadRateMin").value(0.0))
                .andExpect(jsonPath("$.spreadRateMax").value(0.0))
                .andExpect(jsonPath("$.problems[0].problemId").value("00001-00001"))
                .andExpect(jsonPath("$.problems[0].title").value("3월 고객별 주문 건수와 총 주문 금액 조회"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[0].dbms").value("postgresql"));
    }

}
