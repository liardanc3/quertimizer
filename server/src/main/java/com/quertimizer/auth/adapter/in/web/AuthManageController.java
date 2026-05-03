package com.quertimizer.auth.adapter.in.web;

import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.auth.application.port.in.GetAuthManageUseCase;
import com.quertimizer.auth.application.port.in.UpdateUserRoleUseCase;
import com.quertimizer.auth.adapter.in.web.request.AuthManageRoleUpdateReq;
import com.quertimizer.auth.adapter.in.web.response.AuthManageRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthManageController {

    private final GetAuthManageUseCase getAuthManage;
    private final UpdateUserRoleUseCase updateUserRole;

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
     * @param authentication 현재 요청의 인증 정보
     */
    @PutMapping("/admin/auth-manage/users/{handle}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String handle,
                                               @Valid @RequestBody AuthManageRoleUpdateReq request,
                                               Authentication authentication) {
        updateUserRole.execute(UpdateUserRoleInput.of(handle, request.getRole(), authentication.getName(), request.getConfirmationText()));
        return ResponseEntity.ok().build();
    }
}
