package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.endpoint.api.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.endpoint.api.dto.response.AuthManageRes;
import com.quertimizer.service.AuthManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthManageController {

    private final AuthManageService authManageService;

    @GetMapping("/admin/auth-manage")
    public ResponseEntity<AuthManageRes> getAuthManage() {

        // 관리자 권한 현황 조회
        return ResponseEntity.ok(authManageService.getAuthManage());
    }

    @PutMapping("/admin/auth-manage/users/{userId}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String userId,
                                               @Valid @RequestBody AuthManageRoleUpdateReq request) {

        // 관리자 사용자 역할 수정
        authManageService.updateUserRole(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/auth-manage/problem-generators/{userId}/permissions")
    public ResponseEntity<Void> updateProblemGeneratorPermissions(@PathVariable String userId,
                                                                  @RequestBody AuthManageProblemPermissionUpdateReq request) {

        // 관리자 ProblemGenerator 문제 권한 수정
        authManageService.updateProblemGeneratorPermissions(userId, request);
        return ResponseEntity.ok().build();
    }

}
