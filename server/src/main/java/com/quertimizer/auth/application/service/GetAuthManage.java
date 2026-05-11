package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.GetAuthManageUseCase;
import com.quertimizer.auth.application.output.AuthManageOutput;
import com.quertimizer.auth.application.output.AuthManageUserRowOutput;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import com.quertimizer.auth.domain.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAuthManage implements GetAuthManageUseCase {

    private final AuthUserPort userRepository;
    private final AuthManageService authManageService;

    /**
     * 권한 설정 화면 데이터를 조회한다.
     *
     * <ol>
     *   <li>사용자 목록 조회
     *   <li>사용자별 역할 설정 행 응답 조립
     * </ol>
     */
    @Transactional(readOnly = true)
    @Override
    public AuthManageOutput execute() {
        List<AuthManageUserRowOutput> members = userRepository.findAllByOrderByHandleAsc().stream()
                .filter(AuthUser::hasHandle)
                .map(user -> new AuthManageUserRowOutput(user.getHandle(), authManageService.resolveRoleValue(user.getResolvedRole())))
                .toList();
        return new AuthManageOutput(members);
    }
}
