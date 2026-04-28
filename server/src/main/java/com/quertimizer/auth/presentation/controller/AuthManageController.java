package com.quertimizer.auth.presentation.controller;

import com.quertimizer.auth.application.input.UpdateProblemGeneratorPermissionsInput;
import com.quertimizer.auth.application.input.UpdateUserRoleInput;
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

    /**
     * 관리자 권한 관리 화면 데이터를 반환한다.
     */
    @GetMapping("/admin/auth-manage")
    public ResponseEntity<AuthManageRes> getAuthManage() {
        return ResponseEntity.ok(AuthManageRes.from(getAuthManage.execute()));
    }

    /**
     * 관리자가 사용자 역할을 변경한다.
     *
     * @param handle 역할을 변경할 사용자 handle
     * @param request 변경할 역할 요청
     */
    @PutMapping("/admin/auth-manage/users/{handle}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String handle,
                                               @Valid @RequestBody AuthManageRoleUpdateReq request) {
        updateUserRole.execute(UpdateUserRoleInput.of(handle, request.getRole()));
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자가 ProblemGenerator의 문제 권한을 교체한다.
     *
     * @param handle 권한을 변경할 사용자 handle
     * @param request 교체할 권한 key 목록 요청
     */
    @PutMapping("/admin/auth-manage/problem-generators/{handle}/permissions")
    public ResponseEntity<Void> updateProblemGeneratorPermissions(@PathVariable String handle,
                                                                  @RequestBody AuthManageProblemPermissionUpdateReq request) {
        updateProblemGeneratorPermissions.execute(
                UpdateProblemGeneratorPermissionsInput.of(handle, request.getPermissionKeys())
        );
        return ResponseEntity.ok().build();
    }
}
