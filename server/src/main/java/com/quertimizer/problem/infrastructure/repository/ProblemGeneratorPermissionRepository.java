package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemGeneratorPermissionRepository extends JpaRepository<ProblemGeneratorPermission, ProblemGeneratorPermissionId> {

    List<ProblemGeneratorPermission> findAllByOrderByIdHandleAscIdProblemIdAsc();

    List<ProblemGeneratorPermission> findAllByIdHandleOrderByIdProblemIdAsc(String handle);

    void deleteAllByIdHandle(String handle);
}
