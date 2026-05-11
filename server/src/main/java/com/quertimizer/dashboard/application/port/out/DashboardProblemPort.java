package com.quertimizer.dashboard.application.port.out;

import com.quertimizer.dashboard.domain.model.DashboardProblemCandidate;
import com.quertimizer.judge.domain.model.DbmsType;

import java.util.List;

public interface DashboardProblemPort {

    List<DashboardProblemCandidate> findProblemCandidates(DbmsType dbmsType, String currentHandle, int candidateLimit);

}
