package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.output.CommunityDashboardPostCandidateOutput;

import java.util.List;

public interface FindCommunityDashboardPostCandidatesUseCase {

    List<CommunityDashboardPostCandidateOutput> execute();
}
