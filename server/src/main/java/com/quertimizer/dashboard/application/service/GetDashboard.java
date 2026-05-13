package com.quertimizer.dashboard.application.service;

import com.quertimizer.dashboard.application.port.in.GetDashboardUseCase;
import com.quertimizer.dashboard.application.output.DashboardCommunityPostOutput;
import com.quertimizer.dashboard.application.output.DashboardOutput;
import com.quertimizer.dashboard.application.output.DashboardProblemRecommendationOutput;
import com.quertimizer.dashboard.application.port.out.DashboardCommunityPort;
import com.quertimizer.dashboard.application.port.out.DashboardProblemPort;
import com.quertimizer.dashboard.domain.model.DashboardCommunityPostCandidate;
import com.quertimizer.dashboard.domain.model.DashboardProblemCandidate;
import com.quertimizer.dashboard.domain.policy.DashboardHotPostPolicy;
import com.quertimizer.dashboard.domain.policy.DashboardProblemRecommendationPolicy;
import com.quertimizer.global.log.Log;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetDashboard implements GetDashboardUseCase {

    private static final int COMMUNITY_POST_EXCERPT_LENGTH = 720;

    private final DashboardCommunityPort dashboardCommunityPort;
    private final DashboardProblemPort dashboardProblemPort;
    private final DashboardHotPostPolicy hotPostPolicy;
    private final DashboardProblemRecommendationPolicy problemRecommendationPolicy;

    /**
     * 로그인 여부에 맞는 대시보드 데이터를 조회한다.
     *
     * <ol>
     *   <li>인기 커뮤니티 게시글 조회
     *   <li>추천 문제 조회
     *   <li>로그인 상태 포함 대시보드 응답 생성
     * </ol>
     *
     * @param currentHandle 현재 사용자 handle
     */
    @Transactional(readOnly = true)
    @Override
    @Log("대시보드 데이터 조회")
    public DashboardOutput execute(String currentHandle) {
        List<DashboardCommunityPostOutput> hotPosts = getHotCommunityPosts();
        List<DashboardProblemRecommendationOutput> recommendedProblems = getRecommendedProblems(currentHandle);
        return new DashboardOutput(currentHandle != null, currentHandle, hotPosts, recommendedProblems);
    }

    private List<DashboardCommunityPostOutput> getHotCommunityPosts() {
        // 인기 점수순 정렬 후 게시글 응답 변환
        return dashboardCommunityPort.findCommunityPostCandidates().stream()
                .sorted(hotPostPolicy.createHotPostComparator())
                .limit(hotPostPolicy.getDisplayLimit())
                .map(this::toCommunityPostOutput)
                .toList();
    }

    private List<DashboardProblemRecommendationOutput> getRecommendedProblems(String currentHandle) {
        // DBMS별 추천 후보 수집
        Map<String, DashboardProblemCandidate> candidatesByProblemId = new LinkedHashMap<>();
        addProblemCandidates(candidatesByProblemId, DbmsType.POSTGRESQL, currentHandle);
        addProblemCandidates(candidatesByProblemId, DbmsType.MYSQL, currentHandle);

        // 인기순 후보 제한 후 일자 기준 셔플과 응답 변환
        return candidatesByProblemId.values().stream()
                .sorted(problemRecommendationPolicy.createPopularityComparator())
                .limit(problemRecommendationPolicy.getCandidateLimitPerDbms() * 2L)
                .sorted(problemRecommendationPolicy.createDailyShuffleComparator(LocalDate.now()))
                .limit(problemRecommendationPolicy.getDisplayLimit())
                .map(this::toProblemRecommendationOutput)
                .toList();
    }

    private void addProblemCandidates(Map<String, DashboardProblemCandidate> candidatesByProblemId,
                                      DbmsType dbmsType, String currentHandle) {
        // DBMS별 추천 후보 추가
        addProblemCandidates(candidatesByProblemId, dashboardProblemPort.findProblemCandidates(
                dbmsType, currentHandle, problemRecommendationPolicy.getCandidateLimitPerDbms()
        ));
    }

    private void addProblemCandidates(Map<String, DashboardProblemCandidate> candidatesByProblemId,
                                      List<DashboardProblemCandidate> problemCandidates) {
        // 중복 문제 제외 후 추천 후보 추가
        for (DashboardProblemCandidate problemCandidate : problemCandidates) {
            candidatesByProblemId.putIfAbsent(problemCandidate.getProblemId(), problemCandidate);
        }
    }

    private DashboardCommunityPostOutput toCommunityPostOutput(DashboardCommunityPostCandidate post) {
        // 커뮤니티 게시글 응답 변환
        return new DashboardCommunityPostOutput(
                post.getPostId(),
                post.getTitle(), post.getAuthorHandle(), createExcerpt(post.getPlainTextSummary()),
                post.getTags(), post.getCategory(), post.getCreatedAt(),
                post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
                Math.round(hotPostPolicy.calculateHotScore(post) * 10d) / 10d
        );
    }

    private DashboardProblemRecommendationOutput toProblemRecommendationOutput(DashboardProblemCandidate problemEntry) {
        // 문제 추천 응답 변환
        return new DashboardProblemRecommendationOutput(
                problemEntry.getProblemId(), problemEntry.getTitle(),
                problemEntry.getDbms(), problemEntry.getSolvedUserCount(),
                problemEntry.getTotalSubmitCount(), problemEntry.getSuccessSubmitCount(),
                problemEntry.isSolvedByCurrentUser()
        );
    }

    private String createExcerpt(String contentText) {
        // 본문 텍스트 공백 정규화
        String normalizedContentText = normalizeContentText(contentText);

        // 표시할 본문 없으면 빈 문자열 반환
        if (!StringUtils.hasText(normalizedContentText)) {
            return "";
        }

        // 대시보드 표시 길이에 맞춰 요약문 반환
        return normalizedContentText.length() > COMMUNITY_POST_EXCERPT_LENGTH
                ? normalizedContentText.substring(0, COMMUNITY_POST_EXCERPT_LENGTH).trim() + "..."
                : normalizedContentText;
    }

    private String normalizeContentText(String contentText) {
        // 본문 텍스트 정규화
        return StringUtils.hasText(contentText) ? normalizeExcerptWhitespace(contentText) : "";
    }

    private String normalizeExcerptWhitespace(String content) {
        // 요약문 공백 정규화
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

}
