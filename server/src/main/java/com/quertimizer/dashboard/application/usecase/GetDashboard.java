package com.quertimizer.dashboard.application.usecase;

import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.dashboard.application.output.DashboardCommunityPostOutput;
import com.quertimizer.dashboard.application.output.DashboardOutput;
import com.quertimizer.dashboard.application.output.DashboardProblemRecommendationOutput;
import com.quertimizer.dashboard.domain.model.DashboardDisplayConstant;
import com.quertimizer.dashboard.domain.policy.DashboardHotPostPolicy;
import com.quertimizer.dashboard.domain.policy.DashboardProblemRecommendationPolicy;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetDashboard {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final ProblemStore problemStore;
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
    public DashboardOutput execute(String currentHandle) {
        List<DashboardCommunityPostOutput> hotPosts = getHotCommunityPosts();
        List<DashboardProblemRecommendationOutput> recommendedProblems = getRecommendedProblems(currentHandle);
        return new DashboardOutput(currentHandle != null, currentHandle, hotPosts, recommendedProblems);
    }

    private List<DashboardCommunityPostOutput> getHotCommunityPosts() {
        // 인기 게시글과 게시글 태그 조회
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        // 인기 점수순 정렬 후 게시글 응답 변환
        return posts.stream()
                .sorted(hotPostPolicy.createHotPostComparator())
                .limit(hotPostPolicy.getDisplayLimit())
                .map(post -> toCommunityPostOutput(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
    }

    private List<DashboardProblemRecommendationOutput> getRecommendedProblems(String currentHandle) {
        // DBMS별 추천 후보 수집
        Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId = new LinkedHashMap<>();
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

    private void addProblemCandidates(Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId,
                                      DbmsType dbmsType, String currentHandle) {
        // DBMS별 풀이 상태와 정렬 기준별 후보 추가
        String solveState = currentHandle == null ? "all" : "unsolved";
        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "desc", "none", "none"));
        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "none", "desc", "none"));
        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "none", "none", "desc"));
    }

    private List<ProblemStore.ProblemListEntry> findProblemCandidates(DbmsType dbmsType, String currentHandle,
                                                                      String solveState, String solvedCountSort,
                                                                      String totalSubmitSort, String successSubmitSort) {
        // 문제 저장소에서 정렬 기준별 추천 후보 조회
        return problemStore.findProblemPage(
                        1, null, dbmsType, solveState, currentHandle,
                        solvedCountSort, totalSubmitSort, successSubmitSort,
                        "none", null, null
                )
                .problems()
                .stream()
                .limit(problemRecommendationPolicy.getCandidateLimitPerDbms())
                .toList();
    }

    private void addProblemCandidates(Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId,
                                      List<ProblemStore.ProblemListEntry> problemCandidates) {
        // 중복 문제 제외 후 추천 후보 추가
        for (ProblemStore.ProblemListEntry problemCandidate : problemCandidates) {
            candidatesByProblemId.putIfAbsent(problemCandidate.problem().getProblemId(), problemCandidate);
        }
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 태그 결과 저장소 준비
        Map<Long, List<String>> tagsByPostId = new LinkedHashMap<>();

        // 조회 대상 게시글 번호 없으면 빈 결과 반환
        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        // 게시글 번호별 태그 목록 수집
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>()).add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private DashboardCommunityPostOutput toCommunityPostOutput(CommunityPost post, List<String> tags) {
        // 커뮤니티 게시글 응답 변환
        return new DashboardCommunityPostOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(), post.getHandle(), createExcerpt(post.getPlainTextSummary()),
                tags, resolveCategory(post), post.getCreatedAt(),
                post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
                Math.round(hotPostPolicy.calculateHotScore(post) * 10d) / 10d
        );
    }

    private DashboardProblemRecommendationOutput toProblemRecommendationOutput(ProblemStore.ProblemListEntry problemEntry) {
        // 문제 추천 응답 변환
        return new DashboardProblemRecommendationOutput(
                problemEntry.problem().getProblemId(), problemEntry.problem().getTitle(),
                problemEntry.problem().getDbmsType().getValue(), problemEntry.solvedUserCount(),
                problemEntry.totalSubmitCount(), problemEntry.successSubmitCount(),
                problemEntry.spreadRate(), problemEntry.solvedByCurrentUser()
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
        return normalizedContentText.length() > DashboardDisplayConstant.COMMUNITY_POST_EXCERPT_LENGTH
                ? normalizedContentText.substring(0, DashboardDisplayConstant.COMMUNITY_POST_EXCERPT_LENGTH).trim() + "..."
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

    private String resolveCategory(CommunityPost post) {
        // seed 게시글 번호 기준 카테고리 후보 계산
        int postNumber = CommunityPostIdPolicy.resolveSeedPostNumber(post.getPostId()).orElse(0);

        // seed 게시글 번호가 있으면 번호 규칙으로 카테고리 결정
        if (postNumber > 0) {
            if (postNumber % 10 == 0) {
                return "notice";
            }

            return postNumber % 2 == 0 ? "discussion" : "question";
        }

        return "discussion";
    }
}
