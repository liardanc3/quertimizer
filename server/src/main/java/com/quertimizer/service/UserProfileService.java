package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.api.dto.request.UserProfileLinkReq;
import com.quertimizer.endpoint.api.dto.request.UserProfileUpdateReq;
import com.quertimizer.endpoint.api.dto.response.UserProfileLinkRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityCommentRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityCommentsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityPostRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityPostsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSolvedProblemsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSolvedRecordRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSolvedRecordsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSummaryRes;
import com.quertimizer.entity.CommunityComment;
import com.quertimizer.entity.CommunityCommentLike;
import com.quertimizer.entity.CommunityPost;
import com.quertimizer.entity.CommunityPostLike;
import com.quertimizer.entity.CommunityPostTag;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.entity.User;
import com.quertimizer.entity.UserExternalLink;
import com.quertimizer.repository.CommunityCommentLikeRepository;
import com.quertimizer.repository.CommunityCommentRepository;
import com.quertimizer.repository.CommunityPostLikeRepository;
import com.quertimizer.repository.CommunityPostRepository;
import com.quertimizer.repository.CommunityPostTagRepository;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import com.quertimizer.repository.UserExternalLinkRepository;
import com.quertimizer.repository.UserRepository;
import com.quertimizer.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final UserExternalLinkRepository userExternalLinkRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final ProblemStore problemStore;

    public Optional<UserProfileSummaryRes> getProfileSummary(String targetUserId, String currentUserId) {
        boolean isOwnProfile = targetUserId.equals(currentUserId);

        return userRepository.findByUserId(targetUserId)
                .map(user -> buildUserProfileSummary(user, isOwnProfile));
    }

    public Optional<UserProfileSolvedProblemsRes> getSolvedProblems(String targetUserId, String currentUserId) {
        boolean isOwnProfile = targetUserId.equals(currentUserId);

        return userRepository.findByUserId(targetUserId)
                .map(user -> buildSolvedProblems(user, isOwnProfile));
    }

    public Optional<UserProfileSolvedRecordsRes> getSolvedRecords(String targetUserId, String currentUserId) {
        boolean isOwnProfile = targetUserId.equals(currentUserId);

        return userRepository.findByUserId(targetUserId)
                .map(user -> buildSolvedRecords(user, isOwnProfile));
    }

    public Optional<UserProfileCommunityPostsRes> getCommunityPosts(String targetUserId) {
        return userRepository.findByUserId(targetUserId)
                .map(user -> new UserProfileCommunityPostsRes(
                        createCommunityPostResponses(communityPostRepository.findAllByUserIdOrderByCreatedAtDesc(targetUserId))
                ));
    }

    public Optional<UserProfileCommunityPostsRes> getLikedPosts(String targetUserId) {
        return userRepository.findByUserId(targetUserId)
                .map(user -> {
                    List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdUserIdOrderByCreatedAtDesc(targetUserId);
                    Map<String, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(
                                    likedPosts.stream()
                                            .map(CommunityPostLike::getId)
                                            .map(postLikeId -> postLikeId.getPostId())
                                            .distinct()
                                            .toList()
                            ).stream()
                            .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));

                    return new UserProfileCommunityPostsRes(
                            likedPosts.stream()
                                    .map(postLike -> createLikedCommunityPostResponse(postLike, postById))
                                    .flatMap(Optional::stream)
                                    .toList()
                    );
                });
    }

    public Optional<UserProfileCommunityCommentsRes> getCommunityComments(String targetUserId) {
        return userRepository.findByUserId(targetUserId)
                .map(user -> {
                    List<CommunityComment> comments = communityCommentRepository.findAllByUserIdOrderByCreatedAtDesc(targetUserId);
                    Map<String, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                            .map(CommunityComment::getPostId)
                            .distinct()
                            .toList());

                    return new UserProfileCommunityCommentsRes(
                            comments.stream()
                                    .map(comment -> createCommunityCommentResponse(comment, postTitleByPostId, comment.getCreatedAt()))
                                    .toList()
                    );
                });
    }

    public Optional<UserProfileCommunityCommentsRes> getLikedComments(String targetUserId) {
        return userRepository.findByUserId(targetUserId)
                .map(user -> {
                    List<CommunityCommentLike> likedComments = communityCommentLikeRepository.findAllByIdUserIdOrderByCreatedAtDesc(targetUserId);
                    Map<Long, CommunityComment> commentById = communityCommentRepository.findAllByCommentIdIn(
                                    likedComments.stream()
                                            .map(CommunityCommentLike::getId)
                                            .map(commentLikeId -> commentLikeId.getCommentId())
                                            .distinct()
                                            .toList()
                            ).stream()
                            .collect(java.util.stream.Collectors.toMap(CommunityComment::getCommentId, comment -> comment));
                    Map<String, String> postTitleByPostId = createPostTitleByPostId(commentById.values().stream()
                            .map(CommunityComment::getPostId)
                            .distinct()
                            .toList());

                    return new UserProfileCommunityCommentsRes(
                            likedComments.stream()
                                    .map(commentLike -> createLikedCommunityCommentResponse(commentLike, commentById, postTitleByPostId))
                                    .flatMap(Optional::stream)
                                    .toList()
                    );
                });
    }

    public Optional<UserProfileSummaryRes> updateProfile(String userId, UserProfileUpdateReq request) {
        return userRepository.findByUserId(userId)
                .map(user -> {
                    // 소개글, 기본 설정 수정
                    user.changeProfile(
                            normalizeBio(request.getBio()),
                            request.getDefaultDbms(),
                            request.isSqlPublic(),
                            request.isExecutionPercentilePublic(),
                            request.isSolvedRecordsPublic(),
                            request.isSolvedProblemCountPublic()
                    );

                    // 프로필 링크를 입력값으로 교체
                    replaceExternalLinks(user.getUserId(), request.getLinks());

                    return buildUserProfileSummary(user, true);
                });
    }

    private UserProfileSummaryRes buildUserProfileSummary(User user, boolean isOwnProfile) {
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAllByUserIdOrderBySubmittedAtDesc(user.getUserId());
        List<UserExternalLink> externalLinks = userExternalLinkRepository.findAllByIdUserIdOrderByIdTypeAscIdLinkAsc(user.getUserId());

        // 누적 해결 통계 동기화
        syncSolvedStatistics(user, histories);

        // 프로필 기본 정보에 필요한 최고 기록 계산
        List<ProblemSolveHistory> bestSolvedHistories = createBestSolvedHistories(histories);
        boolean executionPercentileVisible = isOwnProfile || user.isExecutionPercentilePublicEnabled();
        long authoredPostCount = communityPostRepository.countByUserId(user.getUserId());
        long likedPostCount = communityPostLikeRepository.countByIdUserId(user.getUserId());
        long commentCount = communityCommentRepository.countByUserId(user.getUserId());

        return new UserProfileSummaryRes(
                user.getUserId(),
                user.getResolvedBio(),
                createProfileLinkResponses(externalLinks),
                user.getResolvedDefaultDbms().getValue(),
                user.isSqlPublicEnabled(),
                user.isExecutionPercentilePublicEnabled(),
                user.isSolvedRecordsPublicEnabled(),
                user.isSolvedProblemCountPublicEnabled(),
                executionPercentileVisible ? calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.POSTGRESQL) : null,
                executionPercentileVisible ? calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.ORACLE) : null,
                authoredPostCount,
                likedPostCount,
                commentCount
        );
    }

    private UserProfileSolvedProblemsRes buildSolvedProblems(User user, boolean isOwnProfile) {
        // 해결한 문제 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedProblemCountPublicEnabled()) {
            return new UserProfileSolvedProblemsRes(0, List.of());
        }

        List<String> solvedProblemIds = createSolvedProblemIds(createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByUserIdOrderBySubmittedAtDesc(user.getUserId())
        ));

        return new UserProfileSolvedProblemsRes(solvedProblemIds.size(), solvedProblemIds);
    }

    private UserProfileSolvedRecordsRes buildSolvedRecords(User user, boolean isOwnProfile) {
        // 해결 기록 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedRecordsPublicEnabled()) {
            return new UserProfileSolvedRecordsRes(List.of());
        }

        List<UserProfileSolvedRecordRes> solvedRecordResponses = createSolvedRecordResponses(createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByUserIdOrderBySubmittedAtDesc(user.getUserId())
        ));

        return new UserProfileSolvedRecordsRes(solvedRecordResponses);
    }

    private void syncSolvedStatistics(User user, List<ProblemSolveHistory> histories) {
        Map<String, ProblemSolveHistory> fastestHistoryByProblemId = new HashMap<>();

        // 문제별 최고 기록 기준 누적 통계 계산
        for (ProblemSolveHistory history : histories) {
            fastestHistoryByProblemId.merge(history.getProblemId(), history, this::pickBetterHistory);
        }

        int solvedProblemCount = fastestHistoryByProblemId.size();
        long solvedExecutionTimeSumMs = fastestHistoryByProblemId.values().stream()
                .mapToLong(ProblemSolveHistory::getExecutionTimeMs)
                .sum();

        if (user.getResolvedSolvedProblemCount() == solvedProblemCount
                && user.getResolvedSolvedExecutionTimeSumMs() == solvedExecutionTimeSumMs) {
            return;
        }

        user.changeSolvedStatistics(solvedProblemCount, solvedExecutionTimeSumMs);
    }

    private List<ProblemSolveHistory> createBestSolvedHistories(List<ProblemSolveHistory> histories) {
        Map<UserSolvedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();

        // 문제, DBMS별 최고 기록만 추출
        for (ProblemSolveHistory history : histories) {
            UserSolvedHistoryKey historyKey = new UserSolvedHistoryKey(
                    history.getProblemId(),
                    resolveDbmsType(history)
            );

            bestHistoryByKey.merge(historyKey, history, this::pickBetterHistory);
        }

        // 최신 순으로 프로필 기록 정렬
        return bestHistoryByKey.values().stream()
                .sorted(Comparator.comparing(ProblemSolveHistory::getSubmittedAt).reversed())
                .toList();
    }

    private List<String> createSolvedProblemIds(List<ProblemSolveHistory> histories) {
        return histories.stream()
                .map(ProblemSolveHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<UserProfileSolvedRecordRes> createSolvedRecordResponses(List<ProblemSolveHistory> bestSolvedHistories) {
        return bestSolvedHistories.stream()
                .map(history -> new UserProfileSolvedRecordRes(
                        history.getProblemId(),
                        problemStore.findProblem(history.getProblemId())
                                .map(problem -> problem.getTitle())
                                .orElse(history.getProblemId()),
                        resolveDbmsType(history).getValue(),
                        history.getExecutionTimeMs(),
                        history.getCost(),
                        history.getSubmittedAt()
                ))
                .toList();
    }

    private Double calculateAverageExecutionPercentile(List<ProblemSolveHistory> bestSolvedHistories, DbmsType dbmsType) {
        List<Integer> executionPercentiles = bestSolvedHistories.stream()
                .filter(history -> resolveDbmsType(history) == dbmsType)
                .map(this::calculateExecutionPercentile)
                .flatMap(Optional::stream)
                .toList();

        if (executionPercentiles.isEmpty()) {
            return null;
        }

        double averageExecutionPercentile = executionPercentiles.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        return Math.round(averageExecutionPercentile * 10d) / 10d;
    }

    private Optional<Integer> calculateExecutionPercentile(ProblemSolveHistory history) {
        List<ProblemSolveHistory> bestSubmittedHistories = problemStore.findBestSubmittedHistories(history.getProblemId()).stream()
                .filter(candidateHistory -> resolveDbmsType(candidateHistory) == resolveDbmsType(history))
                .toList();

        if (bestSubmittedHistories.isEmpty()) {
            return Optional.empty();
        }

        long fasterHistoryCount = bestSubmittedHistories.stream()
                .filter(candidateHistory -> candidateHistory.getExecutionTimeMs() < history.getExecutionTimeMs())
                .count();

        int executionPercentile = Math.max(
                1,
                (int) Math.round(((fasterHistoryCount + 1d) / (bestSubmittedHistories.size() + 1d)) * 100d)
        );

        return Optional.of(executionPercentile);
    }

    private ProblemSolveHistory pickBetterHistory(ProblemSolveHistory currentHistory, ProblemSolveHistory candidateHistory) {
        if (candidateHistory.getCost() < currentHistory.getCost()) {
            return candidateHistory;
        }

        if (candidateHistory.getCost() > currentHistory.getCost()) {
            return currentHistory;
        }

        if (candidateHistory.getExecutionTimeMs() < currentHistory.getExecutionTimeMs()) {
            return candidateHistory;
        }

        if (candidateHistory.getExecutionTimeMs() > currentHistory.getExecutionTimeMs()) {
            return currentHistory;
        }

        if (candidateHistory.getSubmittedAt().isBefore(currentHistory.getSubmittedAt())) {
            return candidateHistory;
        }

        return currentHistory;
    }

    private List<UserProfileLinkRes> createProfileLinkResponses(List<UserExternalLink> externalLinks) {
        return externalLinks.stream()
                .map(link -> new UserProfileLinkRes(link.getType(), link.getLink()))
                .toList();
    }

    private List<UserProfileCommunityPostRes> createCommunityPostResponses(List<CommunityPost> posts) {
        return posts.stream()
                .map(this::createCommunityPostResponse)
                .toList();
    }

    private UserProfileCommunityPostRes createCommunityPostResponse(CommunityPost post) {
        return new UserProfileCommunityPostRes(
                post.getPostId(),
                post.getTitle(),
                createCommunityExcerpt(post.getContentText()),
                communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(post.getPostId()).stream()
                        .map(CommunityPostTag::getTag)
                        .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount()
        );
    }

    private Optional<UserProfileCommunityPostRes> createLikedCommunityPostResponse(CommunityPostLike postLike, Map<String, CommunityPost> postById) {
        CommunityPost post = postById.get(postLike.getId().getPostId());

        if (post == null) {
            return Optional.empty();
        }

        return Optional.of(new UserProfileCommunityPostRes(
                post.getPostId(),
                post.getTitle(),
                createCommunityExcerpt(post.getContentText()),
                communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(post.getPostId()).stream()
                        .map(CommunityPostTag::getTag)
                        .toList(),
                postLike.getCreatedAt(),
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount()
        ));
    }

    private Optional<UserProfileCommunityCommentRes> createLikedCommunityCommentResponse(CommunityCommentLike commentLike,
                                                                                          Map<Long, CommunityComment> commentById,
                                                                                          Map<String, String> postTitleByPostId) {
        CommunityComment comment = commentById.get(commentLike.getId().getCommentId());

        if (comment == null) {
            return Optional.empty();
        }

        return Optional.of(createCommunityCommentResponse(comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private UserProfileCommunityCommentRes createCommunityCommentResponse(CommunityComment comment,
                                                                          Map<String, String> postTitleByPostId,
                                                                          java.time.LocalDateTime actedAt) {
        return new UserProfileCommunityCommentRes(
                comment.getCommentId(),
                comment.getPostId(),
                postTitleByPostId.getOrDefault(comment.getPostId(), comment.getPostId()),
                comment.getContent(),
                actedAt,
                comment.getParentCommentId() != null
        );
    }

    private Map<String, String> createPostTitleByPostId(List<String> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return communityPostRepository.findAllByPostIdIn(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, CommunityPost::getTitle));
    }

    private String createCommunityExcerpt(String contentText) {
        if (contentText == null || contentText.isBlank()) {
            return "";
        }

        String normalizedContentText = contentText.trim();
        return normalizedContentText.length() > 120
                ? normalizedContentText.substring(0, 120).trim() + "..."
                : normalizedContentText;
    }

    private void replaceExternalLinks(String userId, List<UserProfileLinkReq> links) {
        List<UserExternalLink> normalizedExternalLinks = normalizeExternalLinks(userId, links);

        // 사용자 링크를 최신 입력값으로 전체 교체
        userExternalLinkRepository.deleteAllByIdUserId(userId);

        if (normalizedExternalLinks.isEmpty()) {
            return;
        }

        userExternalLinkRepository.saveAll(normalizedExternalLinks);
    }

    private List<UserExternalLink> normalizeExternalLinks(String userId, List<UserProfileLinkReq> links) {
        Map<String, UserExternalLink> externalLinkByKey = new LinkedHashMap<>();

        // 공백, 중복 링크를 제거하고 저장용 엔티티 구성
        for (UserProfileLinkReq link : links) {
            String type = link.getType().trim();
            String value = link.getValue().trim();

            if (type.isEmpty() || value.isEmpty()) {
                continue;
            }

            externalLinkByKey.putIfAbsent(type + "|" + value, UserExternalLink.create(userId, type, value));
        }

        return new ArrayList<>(externalLinkByKey.values());
    }

    private String normalizeBio(String bio) {
        return bio != null ? bio.trim() : "";
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private record UserSolvedHistoryKey(String problemId, DbmsType dbmsType) {
    }

}
