package com.quertimizer.repository;

import com.quertimizer.entity.ProblemGeneratorPermission;
import com.quertimizer.entity.ProblemGeneratorPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemGeneratorPermissionRepository extends JpaRepository<ProblemGeneratorPermission, ProblemGeneratorPermissionId> {

    List<ProblemGeneratorPermission> findAllByOrderByIdUserIdAscIdProblemIdAsc();

    void deleteAllByIdUserId(String userId);
}
