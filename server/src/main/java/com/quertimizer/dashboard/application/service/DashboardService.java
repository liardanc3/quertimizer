package com.quertimizer.dashboard.application.service;

import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.infrastructure.repository.CommunityPostRepository;
import com.quertimizer.community.infrastructure.repository.CommunityPostTagRepository;
import com.quertimizer.dashboard.application.result.DashboardCommunityPostResult;
import com.quertimizer.dashboard.application.result.DashboardProblemRecommendationResult;
import com.quertimizer.dashboard.application.result.DashboardResult;
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
    private static final String LEADING_DETAIL_HEADING_BLOCK_PATTERN =
            "(?is)^\\s*(<br\\s*/?>|<p[^>]*>(?:\\s|&nbsp;|<br\\s*/?>)*</p>|<h[1-3][^>]*>.*?</h[1-3]>)";

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final ProblemStore problemStore;
    private final DashboardHotPostPolicy hotPostPolicy;
    private final DashboardProblemRecommendationPolicy problemRecommendationPolicy;

    public DashboardResult getDashboard(String currentHandle) {
        return new DashboardResult(
                currentHandle != null,
                currentHandle,
                getHotCommunityPosts(),
                getRecommendedProblems(currentHandle)
        );
    }

    private List<DashboardCommunityPostResult> getHotCommunityPosts() {
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<String, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        return posts.stream()
                .sorted(hotPostPolicy.createHotPostComparator())
                .limit(hotPostPolicy.getDisplayLimit())
                .map(post -> toCommunityPostResult(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
    }

    private List<DashboardProblemRecommendationResult> getRecommendedProblems(String currentHandle) {
        Map<String, ProblemStore.ProblemListEntry> candidatesByProblemId = new LinkedHashMap<>();

        addProblemCandidates(candidatesByProblemId, DbmsType.POSTGRESQL, currentHandle);
        addProblemCandidates(candidatesByProblemId, DbmsType.ORACLE, currentHandle);

        return candidatesByProblemId.values().stream()
                .sorted(problemRecommendationPolicy.createPopularityComparator())
                .limit(problemRecommendationPolicy.getCandidateLimitPerDbms() * 2L)
                .sorted(problemRecommendationPolicy.createDailyShuffleComparator(LocalDate.now()))
                .limit(problemRecommendationPolicy.getDisplayLimit())
                .map(this::toProblemRecommendationResult)
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

    private Map<String, List<String>> createTagsByPostId(List<String> postIds) {
        Map<String, List<String>> tagsByPostId = new LinkedHashMap<>();

        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>())
                    .add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private DashboardCommunityPostResult toCommunityPostResult(CommunityPost post, List<String> tags) {
        return new DashboardCommunityPostResult(
                post.getPostId(),
                post.getTitle(),
                post.getHandle(),
                createExcerpt(post.getContentHtml(), post.getContentText()),
                tags,
                resolveCategory(post),
                post.getCreatedAt(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                Math.round(hotPostPolicy.calculateHotScore(post) * 10d) / 10d
        );
    }

    private DashboardProblemRecommendationResult toProblemRecommendationResult(ProblemStore.ProblemListEntry problemEntry) {
        return new DashboardProblemRecommendationResult(
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

    private String createExcerpt(String contentHtml, String contentText) {
        String normalizedContentText = StringUtils.hasText(contentHtml) ? stripContentHtmlForExcerpt(contentHtml) : normalizeContentText(contentText);

        if (!StringUtils.hasText(normalizedContentText)) {
            return "";
        }

        return normalizedContentText.length() > EXCERPT_LENGTH
                ? normalizedContentText.substring(0, EXCERPT_LENGTH).trim() + "..."
                : normalizedContentText;
    }

    private String stripContentHtmlForExcerpt(String contentHtml) {
        return normalizeExcerptWhitespace(stripLeadingHeadingBlocks(contentHtml).trim()
                .replaceAll("(?i)<img[^>]*>", " [이미지] ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h1|h2|h3|blockquote|figure|figcaption)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " "));
    }

    private String stripLeadingHeadingBlocks(String contentHtml) {
        String strippedContentHtml = contentHtml;
        String nextContentHtml = strippedContentHtml.replaceFirst(LEADING_DETAIL_HEADING_BLOCK_PATTERN, "");

        while (!nextContentHtml.equals(strippedContentHtml)) {
            strippedContentHtml = nextContentHtml;
            nextContentHtml = strippedContentHtml.replaceFirst(LEADING_DETAIL_HEADING_BLOCK_PATTERN, "");
        }

        return strippedContentHtml;
    }

    private String normalizeContentText(String contentText) {
        return StringUtils.hasText(contentText) ? normalizeExcerptWhitespace(contentText) : "";
    }

    private String normalizeExcerptWhitespace(String content) {
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String resolveCategory(CommunityPost post) {
        int postNumber = extractPostNumber(post.getPostId());

        if (postNumber > 0) {
            if (postNumber % 10 == 0) {
                return "notice";
            }

            return postNumber % 2 == 0 ? "discussion" : "question";
        }

        return "discussion";
    }

    private int extractPostNumber(String postId) {
        if (!StringUtils.hasText(postId)) {
            return 0;
        }

        String digits = postId.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digits)) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

}
