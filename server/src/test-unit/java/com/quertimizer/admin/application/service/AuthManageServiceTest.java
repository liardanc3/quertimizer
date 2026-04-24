package com.quertimizer.admin.application.service;

import com.quertimizer.admin.domain.model.AuthManageFailReason;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.admin.presentation.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.admin.presentation.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.admin.presentation.dto.response.AuthManageRes;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.infrastructure.repository.ProblemGeneratorPermissionJpaRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemJpaRepository;
import com.quertimizer.user.infrastructure.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthManageServiceTest {

    @InjectMocks
    private AuthManageService authManageService;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private ProblemJpaRepository problemRepository;

    @Mock
    private ProblemGeneratorPermissionJpaRepository problemGeneratorPermissionRepository;

    @Test
    @DisplayName("권한 그룹별 사용자 수와 ProblemGenerator 문제 권한을 묶어서 반환한다.")
    void returnsGroupedAuthManageSummary() {
        // given
        User admin = createUser("admin", UserRole.ADMIN);
        User user = createUser("user01", UserRole.USER);
        User problemGenerator01 = createUser("problemgen01", UserRole.PROBLEM_GENERATOR);
        User problemGenerator02 = createUser("problemgen02", UserRole.PROBLEM_GENERATOR);

        when(userRepository.findAllByOrderByHandleAsc()).thenReturn(List.of(admin, problemGenerator01, problemGenerator02, user));
        when(problemGeneratorPermissionRepository.findAllByOrderByIdHandleAscIdProblemIdAsc()).thenReturn(List.of(
                ProblemGeneratorPermission.create("problemgen01", "00001-00001"),
                ProblemGeneratorPermission.create("problemgen01", "00001-00002"),
                ProblemGeneratorPermission.create("problemgen02", "00002-00001")
        ));

        // when
        AuthManageRes response = authManageService.getAuthManage();

        // then
        assertEquals(1, response.getAdmins().getCount());
        assertEquals("admin", response.getAdmins().getMembers().get(0).getHandle());

        assertEquals(1, response.getUsers().getCount());
        assertEquals("user01", response.getUsers().getMembers().get(0).getHandle());

        assertEquals(2, response.getProblemGenerators().getCount());
        assertEquals("problemgen01", response.getProblemGenerators().getMembers().get(0).getHandle());
        assertEquals(List.of("00001-00001", "00001-00002"), response.getProblemGenerators().getMembers().get(0).getProblemIds());
        assertEquals("problemgen02", response.getProblemGenerators().getMembers().get(1).getHandle());
        assertEquals(List.of("00002-00001"), response.getProblemGenerators().getMembers().get(1).getProblemIds());
    }

    @Test
    @DisplayName("마지막 Admin 역할은 해제할 수 없다.")
    void rejectWhenRemovingLastAdminRole() {
        // given
        User admin = createUser("admin", UserRole.ADMIN);
        AuthManageRoleUpdateReq request = createRoleUpdateRequest("user");

        when(userRepository.findById("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findAllByOrderByHandleAsc()).thenReturn(List.of(admin));

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> authManageService.updateUserRole("admin", request));

        // then
        assertEquals(AuthManageFailReason.LAST_ADMIN_PROTECTION.getMessage(), exception.getReason());
        verify(problemGeneratorPermissionRepository, never()).deleteAllByIdHandle("admin");
    }

    @Test
    @DisplayName("ProblemGenerator 외 역할로 바꾸면 문제 권한을 제거한다.")
    void clearProblemPermissionsWhenRoleChangesToNonProblemGenerator() {
        // given
        User problemGenerator = createUser("problemgen01", UserRole.PROBLEM_GENERATOR);
        AuthManageRoleUpdateReq request = createRoleUpdateRequest("user");

        when(userRepository.findById("problemgen01")).thenReturn(Optional.of(problemGenerator));

        // when
        authManageService.updateUserRole("problemgen01", request);

        // then
        assertEquals(UserRole.USER, problemGenerator.getResolvedRole());
        verify(problemGeneratorPermissionRepository).deleteAllByIdHandle("problemgen01");
    }

    @Test
    @DisplayName("ProblemGenerator 문제 권한을 저장한다.")
    void saveProblemGeneratorPermissions() {
        // given
        User problemGenerator = createUser("problemgen01", UserRole.PROBLEM_GENERATOR);
        AuthManageProblemPermissionUpdateReq request = createProblemPermissionUpdateRequest(List.of("00001-00001", "00001-00002", "00001-00001"));

        when(userRepository.findById("problemgen01")).thenReturn(Optional.of(problemGenerator));
        when(problemRepository.findAllById(List.of("00001-00001", "00001-00002"))).thenReturn(List.of(
                Problem.create("00001-00001", "problem-01", "description"),
                Problem.create("00001-00002", "problem-02", "description")
        ));

        // when
        authManageService.updateProblemGeneratorPermissions("problemgen01", request);

        // then
        ArgumentCaptor<List<ProblemGeneratorPermission>> permissionCaptor = ArgumentCaptor.forClass(List.class);

        verify(problemGeneratorPermissionRepository).deleteAllByIdHandle("problemgen01");
        verify(problemGeneratorPermissionRepository).saveAll(permissionCaptor.capture());
        assertEquals(List.of("00001-00001", "00001-00002"), permissionCaptor.getValue().stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .toList());
    }

    @Test
    @DisplayName("ProblemGenerator가 아니면 문제 권한을 수정할 수 없다.")
    void rejectWhenUserIsNotProblemGenerator() {
        // given
        User user = createUser("user01", UserRole.USER);
        AuthManageProblemPermissionUpdateReq request = createProblemPermissionUpdateRequest(List.of("00001-00001"));

        when(userRepository.findById("user01")).thenReturn(Optional.of(user));

        // when
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authManageService.updateProblemGeneratorPermissions("user01", request));

        // then
        assertEquals(AuthManageFailReason.PROBLEM_GENERATOR_REQUIRED.getMessage(), exception.getReason());
        verify(problemGeneratorPermissionRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private User createUser(String handle, UserRole role) {
        User user = User.create(handle, "password", handle + "@example.com");

        user.changeRole(role);
        return user;
    }

    private AuthManageRoleUpdateReq createRoleUpdateRequest(String role) {
        AuthManageRoleUpdateReq request = new AuthManageRoleUpdateReq();

        setField(request, "role", role);
        return request;
    }

    private AuthManageProblemPermissionUpdateReq createProblemPermissionUpdateRequest(List<String> problemIds) {
        AuthManageProblemPermissionUpdateReq request = new AuthManageProblemPermissionUpdateReq();

        setField(request, "problemIds", problemIds);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
