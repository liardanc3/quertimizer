package com.quertimizer.dashboard.application.service;

import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.dashboard.application.output.DashboardCommunityPostOutput;
import com.quertimizer.dashboard.application.output.DashboardProblemRecommendationOutput;
import com.quertimizer.dashboard.application.output.DashboardOutput;
import com.quertimizer.dashboard.domain.policy.DashboardHotPostPolicy;
import com.quertimizer.dashboard.domain.policy.DashboardProblemRecommendationPolicy;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int EXCERPT_LENGTH = 720;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final ProblemStore problemStore;
    private final DashboardHotPostPolicy hotPostPolicy;
    private final DashboardProblemRecommendationPolicy problemRecommendationPolicy;

    public DashboardOutput getDashboard(String currentHandle) {
        // 대시보드에 필요한 게시글과 추천 문제를 조회
        return new DashboardOutput(
                currentHandle != null,
                currentHandle,
                getHotCommunityPosts(),
                getRecommendedProblems(currentHandle)
        );
    }

    private List<DashboardCommunityPostOutput> getHotCommunityPosts() {
        // 인기 커뮤니티 게시글 목록 조회
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        return posts.stream()
                .sorted(hotPostPolicy.createHotPostComparator())
                .limit(hotPostPolicy.getDisplayLimit())
                .map(post -> toCommunityPostOutput(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
    }

    private List<DashboardProblemRecommendationOutput> getRecommendedProblems(String currentHandle) {
        // 추천 문제 목록 조회
        Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId = new LinkedHashMap<>();

        addProblemCandidates(candidatesByProblemId, DbmsType.POSTGRESQL, currentHandle);
        addProblemCandidates(candidatesByProblemId, DbmsType.ORACLE, currentHandle);

        return candidatesByProblemId.values().stream()
                .sorted(problemRecommendationPolicy.createPopularityComparator())
                .limit(problemRecommendationPolicy.getCandidateLimitPerDbms() * 2L)
                .sorted(problemRecommendationPolicy.createDailyShuffleComparator(LocalDate.now()))
                .limit(problemRecommendationPolicy.getDisplayLimit())
                .map(this::toProblemRecommendationOutput)
                .toList();
    }

    private void addProblemCandidates(Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId,
                                      DbmsType dbmsType,
                                      String currentHandle) {
        String solveState = currentHandle == null ? "all" : "unsolved";

        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "desc", "none", "none"));
        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "none", "desc", "none"));
        addProblemCandidates(candidatesByProblemId, findProblemCandidates(dbmsType, currentHandle, solveState, "none", "none", "desc"));
    }

    private List<ProblemStore.ProblemListEntry> findProblemCandidates(DbmsType dbmsType,
                                                                      String currentHandle,
                                                                      String solveState,
                                                                      String solvedCountSort,
                                                                      String totalSubmitSort,
                                                                      String successSubmitSort) {
        return problemStore.findProblemPage(
                        1,
                        null,
                        dbmsType,
                        solveState,
                        currentHandle,
                        solvedCountSort,
                        totalSubmitSort,
                        successSubmitSort,
                        "none",
                        null,
                        null
                )
                .problems()
                .stream()
                .limit(problemRecommendationPolicy.getCandidateLimitPerDbms())
                .toList();
    }

    private void addProblemCandidates(Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId,
                                      List<ProblemStore.ProblemListEntry> problemCandidates) {
        for (ProblemStore.ProblemListEntry problemCandidate : problemCandidates) {
            candidatesByProblemId.putIfAbsent(problemCandidate.problem().getProblemId(), problemCandidate);
        }
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 번호별 태그 목록 생성
        Map<Long, List<String>> tagsByPostId = new LinkedHashMap<>();

        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>())
                    .add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private DashboardCommunityPostOutput toCommunityPostOutput(CommunityPost post, List<String> tags) {
        // 커뮤니티 게시글 응답으로 변환
        return new DashboardCommunityPostOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(),
                post.getHandle(),
                createExcerpt(post.getPlainTextSummary()),
                tags,
                resolveCategory(post),
                post.getCreatedAt(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                Math.round(hotPostPolicy.calculateHotScore(post) * 10d) / 10d
        );
    }

    private DashboardProblemRecommendationOutput toProblemRecommendationOutput(ProblemStore.ProblemListEntry problemEntry) {
        // 문제 추천 응답으로 변환
        return new DashboardProblemRecommendationOutput(
                problemEntry.problem().getProblemId(),
                problemEntry.problem().getTitle(),
                problemEntry.problem().getDbmsType().getValue(),
                problemEntry.solvedUserCount(),
                problemEntry.totalSubmitCount(),
                problemEntry.successSubmitCount(),
                problemEntry.spreadRate(),
                problemEntry.solvedByCurrentUser()
        );
    }

    private String createExcerpt(String contentText) {
        // 요약문 생성
        String normalizedContentText = normalizeContentText(contentText);

        if (!StringUtils.hasText(normalizedContentText)) {
            return "";
        }

        return normalizedContentText.length() > EXCERPT_LENGTH
                ? normalizedContentText.substring(0, EXCERPT_LENGTH).trim() + "..."
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
        // 구분 결정
        int postNumber = CommunityPostIdPolicy.resolveSeedPostNumber(post.getPostId()).orElse(0);

        if (postNumber > 0) {
            if (postNumber % 10 == 0) {
                return "notice";
            }

            return postNumber % 2 == 0 ? "discussion" : "question";
        }

        return "discussion";
    }

}
