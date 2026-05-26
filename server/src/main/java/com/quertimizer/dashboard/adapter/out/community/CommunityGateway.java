package com.quertimizer.dashboard.adapter.out.community;

import com.quertimizer.community.application.output.CommunityDashboardPostCandidateOutput;
import com.quertimizer.community.application.port.in.FindCommunityDashboardPostCandidatesUseCase;
import com.quertimizer.dashboard.application.port.out.DashboardCommunityPort;
import com.quertimizer.dashboard.domain.model.DashboardCommunityPostCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dashboardCommunityGateway")
@RequiredArgsConstructor
public class CommunityGateway implements DashboardCommunityPort {

    private final FindCommunityDashboardPostCandidatesUseCase findCommunityDashboardPostCandidates;

    @Override
    public List<DashboardCommunityPostCandidate> findCommunityPostCandidates() {
        // community 공개 use case 기준 대시보드 후보 모델 변환
        return findCommunityDashboardPostCandidates.execute().stream()
                .map(this::toCandidate)
                .toList();
    }

    private DashboardCommunityPostCandidate toCandidate(CommunityDashboardPostCandidateOutput output) {
        // community 후보 응답을 dashboard 후보 모델로 변환
        return new DashboardCommunityPostCandidate(
                output.getPostId(), output.getTitle(), output.getHandle(),
                output.getContentJson(), output.getPlainTextSummary(), output.getTags(), output.getCategory(), output.getCreatedAt(),
                output.getViewCount(), output.getLikeCount(), output.getCommentCount()
        );
    }
}
