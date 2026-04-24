package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.usecase.GetAuthManage;
import com.quertimizer.auth.application.usecase.UpdateProblemGeneratorPermissions;
import com.quertimizer.auth.application.usecase.UpdateUserRole;
import com.quertimizer.auth.presentation.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.auth.presentation.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.auth.presentation.dto.response.AuthManageRes;
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

    private final GetAuthManage getAuthManage;
    private final UpdateUserRole updateUserRole;
    private final UpdateProblemGeneratorPermissions updateProblemGeneratorPermissions;

    @GetMapping("/admin/auth-manage")
    public ResponseEntity<AuthManageRes> getAuthManage() {
        // 관리자 권한 현황 조회
        return ResponseEntity.ok(AuthManageRes.from(getAuthManage.execute()));
    }

    @PutMapping("/admin/auth-manage/users/{handle}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String handle,
                                               @Valid @RequestBody AuthManageRoleUpdateReq request) {
        // 관리자 사용자 역할 수정
        updateUserRole.execute(handle, request.getRole());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/auth-manage/problem-generators/{handle}/permissions")
    public ResponseEntity<Void> updateProblemGeneratorPermissions(@PathVariable String handle,
                                                                  @RequestBody AuthManageProblemPermissionUpdateReq request) {
        // 관리자 ProblemGenerator 문제 권한 수정
        updateProblemGeneratorPermissions.execute(handle, request.getPermissionKeys());
        return ResponseEntity.ok().build();
    }
}
