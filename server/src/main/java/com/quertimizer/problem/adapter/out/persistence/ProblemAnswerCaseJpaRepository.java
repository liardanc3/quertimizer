package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.domain.model.ProblemAnswerCaseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemAnswerCaseJpaRepository extends JpaRepository<ProblemAnswerCaseJpaEntity, Long> {

    Optional<ProblemAnswerCaseJpaEntity> findFirstByProblemIdAndCaseTypeOrderByCaseOrderAsc(String problemId,
                                                                                            ProblemAnswerCaseType caseType);

    List<ProblemAnswerCaseJpaEntity> findAllByProblemIdAndCaseTypeOrderByCaseOrderAsc(String problemId,
                                                                                       ProblemAnswerCaseType caseType);

    void deleteAllByProblemId(String problemId);
}
