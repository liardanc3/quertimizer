package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, String> {
}
