package com.quertimizer.dashboard.application.port.out;

import com.quertimizer.dashboard.domain.model.DashboardCommunityPostCandidate;

import java.util.List;

public interface DashboardCommunityPort {

    List<DashboardCommunityPostCandidate> findCommunityPostCandidates();

}
