package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.response.AuthManageMemberRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageProblemGeneratorGroupRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageProblemGeneratorMemberRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageRoleGroupRes;
import com.quertimizer.log.LogFormatter;
import com.quertimizer.service.AuthManageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthManageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthManageService authManageService;

    @MockitoBean
    private LogFormatter logFormatter;

    @Test
    @DisplayName("GET /admin/auth-manage : 200 OK + 권한 현황 반환")
    void okAndReturnAuthManageSummary() throws Exception {
        // given
        when(authManageService.getAuthManage()).thenReturn(new AuthManageRes(
                new AuthManageRoleGroupRes(2, List.of(
                        new AuthManageMemberRes("admin"),
                        new AuthManageMemberRes("liardanc3")
                )),
                new AuthManageRoleGroupRes(1, List.of(
                        new AuthManageMemberRes("beginner01")
                )),
                new AuthManageProblemGeneratorGroupRes(1, List.of(
                        new AuthManageProblemGeneratorMemberRes("problemgen01", List.of("00001-00001"))
                ))
        ));

        // when & then
        mockMvc.perform(get("/admin/auth-manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admins.count").value(2))
                .andExpect(jsonPath("$.admins.members[0].userId").value("admin"))
                .andExpect(jsonPath("$.users.count").value(1))
                .andExpect(jsonPath("$.users.members[0].userId").value("beginner01"))
                .andExpect(jsonPath("$.problemGenerators.count").value(1))
                .andExpect(jsonPath("$.problemGenerators.members[0].userId").value("problemgen01"))
                .andExpect(jsonPath("$.problemGenerators.members[0].problemIds[0]").value("00001-00001"));
    }

    @Test
    @DisplayName("PUT /admin/auth-manage/users/{userId}/role : 200 OK + 역할 수정")
    void okAndUpdateUserRole() throws Exception {
        // when & then
        mockMvc.perform(put("/admin/auth-manage/users/problemgen01/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authManageService).updateUserRole(eq("problemgen01"), any());
    }

    @Test
    @DisplayName("PUT /admin/auth-manage/problem-generators/{userId}/permissions : 200 OK + 문제 권한 수정")
    void okAndUpdateProblemGeneratorPermissions() throws Exception {
        // when & then
        mockMvc.perform(put("/admin/auth-manage/problem-generators/problemgen01/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemIds": ["00001-00001", "00001-00002"]
                                }
                                """))
                .andExpect(status().isOk());

        verify(authManageService).updateProblemGeneratorPermissions(eq("problemgen01"), any());
    }

}
