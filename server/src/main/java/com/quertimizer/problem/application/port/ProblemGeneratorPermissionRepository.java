package com.quertimizer.problem.application.port;

import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;

import java.util.List;

public interface ProblemGeneratorPermissionRepository {

    List<ProblemGeneratorPermission> findAllByOrderByIdHandleAscIdProblemIdAsc();

    List<ProblemGeneratorPermission> findAllByIdHandleOrderByIdProblemIdAsc(String handle);

    void deleteAllByIdHandle(String handle);

    <S extends ProblemGeneratorPermission> S save(S problemGeneratorPermission);

    <S extends ProblemGeneratorPermission> List<S> saveAll(Iterable<S> problemGeneratorPermissions);
}
