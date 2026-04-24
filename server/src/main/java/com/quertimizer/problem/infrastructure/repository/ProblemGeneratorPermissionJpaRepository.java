package com.quertimizer.problem.infrastructure.repository;

import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemGeneratorPermissionJpaRepository extends JpaRepository<ProblemGeneratorPermission, ProblemGeneratorPermissionId>, ProblemGeneratorPermissionRepository {
}
