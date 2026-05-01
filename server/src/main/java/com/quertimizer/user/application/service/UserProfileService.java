package com.quertimizer.user.application.service;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.user.application.input.UserProfileLinkInput;
import com.quertimizer.user.application.input.UserProfileUpdateInput;
import com.quertimizer.user.application.output.UserProfileLinkOutput;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.output.UserProfileCommunityActivityOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionActivityOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.domain.entity.UserExternalLink;
import com.quertimizer.user.domain.model.UserProfilePageConstant;
import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.user.application.port.UserExternalLinkRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
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

    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;
    private final UserExternalLinkRepository userExternalLinkRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final ProblemStore problemStore;

    public UserProfileSummaryOutput buildUserProfileSummary(User user, boolean isOwnProfile) {
        // 사용자 프로필 요약 구성
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(user.getHandle());
        List<UserExternalLink> externalLinks = userExternalLinkRepository.findAllByIdHandleOrderByIdTypeAscIdLinkAsc(user.getHandle());

        // 누적 해결 통계 동기화
        syncSolvedStatistics(user, histories);

        // 프로필 기본 정보에 필요한 최고 기록 계산
        List<ProblemSolveHistory> bestSolvedHistories = createBestSolvedHistories(histories);
        boolean executionPercentileVisible = isOwnProfile || user.isExecutionPercentilePublicEnabled();
        boolean communityActivityVisible = isOwnProfile || user.isCommunityActivityPublicEnabled();
        long authoredPostCount = communityActivityVisible ? communityPostRepository.countByHandle(user.getHandle()) : 0;
        long likedPostCount = communityActivityVisible ? communityPostLikeRepository.countByIdHandle(user.getHandle()) : 0;
        long commentCount = communityActivityVisible ? communityCommentRepository.countByHandle(user.getHandle()) : 0;

        return new UserProfileSummaryOutput(
                user.getHandle(),
                user.getResolvedBio(),
                user.getResolvedProfileImageUrl(),
                user.getResolvedBackgroundImageUrl(),
                user.getSignupAt(),
                createProfileLinkResponses(externalLinks),
                user.getResolvedDefaultDbms().getValue(),
                user.isSqlPublicEnabled(),
                user.isExecutionPercentilePublicEnabled(),
                user.isSolvedRecordsPublicEnabled(),
                user.isSolvedProblemCountPublicEnabled(),
                user.isCommunityActivityPublicEnabled(),
                executionPercentileVisible ? calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.POSTGRESQL) : null,
                executionPercentileVisible ? calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.MYSQL) : null,
                authoredPostCount,
                likedPostCount,
                commentCount
        );
    }

    public UserProfileSolvedProblemsOutput buildSolvedProblems(User user, boolean isOwnProfile) {
        // 해결한 문제 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedProblemCountPublicEnabled()) {
            return new UserProfileSolvedProblemsOutput(0, List.of());
        }

        List<String> solvedProblemIds = createSolvedProblemIds(createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(user.getHandle())
        ));

        return new UserProfileSolvedProblemsOutput(solvedProblemIds.size(), solvedProblemIds);
    }

    public UserProfileSolvedRecordsOutput buildSolvedRecords(User user, boolean isOwnProfile) {
        // 해결 기록 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedRecordsPublicEnabled()) {
            return new UserProfileSolvedRecordsOutput(List.of());
        }

        List<UserProfileSolvedRecordOutput> solvedRecordResponses = createSolvedRecordResponses(createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(user.getHandle())
        ));

        return new UserProfileSolvedRecordsOutput(solvedRecordResponses);
    }

    public UserProfileSubmissionSummaryOutput buildSubmissionSummary(User user, boolean isOwnProfile) {
        // 프로필 본문에 필요한 제출 요약 정보를 구성
        List<ProblemSubmitHistory> submitHistories = problemSubmitHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(user.getHandle());
        return new UserProfileSubmissionSummaryOutput(
                createAttemptedProblemIds(submitHistories),
                createSubmissionActivityOutputs(submitHistories)
        );
    }

    public UserProfileCommunityPostsOutput buildCommunityPosts(User user, String currentHandle) {
        // 커뮤니티 활동 공개 여부에 따른 작성 게시글 목록 생성
        return new UserProfileCommunityPostsOutput(
                canShowCommunityActivity(user, currentHandle)
                        ? createCommunityPostResponses(communityPostRepository.findAllByHandleOrderByCreatedAtDesc(user.getHandle()))
                        : List.of()
        );
    }

    public UserProfileCommunityPostsOutput buildLikedPosts(User user, String currentHandle) {
        // 커뮤니티 활동 비공개면 빈 좋아요 게시글 목록 반환
        if (!canShowCommunityActivity(user, currentHandle)) {
            return new UserProfileCommunityPostsOutput(List.of());
        }

        // 좋아요한 게시글 목록 조회 후 응답 변환
        List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(user.getHandle());
        Map<Long, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(likedPosts.stream()
                        .map(CommunityPostLike::getId)
                        .map(postLikeId -> postLikeId.getPostId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));
        return new UserProfileCommunityPostsOutput(likedPosts.stream()
                .map(postLike -> createLikedCommunityPostResponse(postLike, postById))
                .flatMap(Optional::stream)
                .toList());
    }

    public UserProfileCommunityCommentsOutput buildCommunityComments(User user, String currentHandle) {
        // 커뮤니티 활동 비공개면 빈 댓글 목록 반환
        if (!canShowCommunityActivity(user, currentHandle)) {
            return new UserProfileCommunityCommentsOutput(List.of());
        }

        // 작성 댓글 목록 조회 후 응답 변환
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(user.getHandle());
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());
        return new UserProfileCommunityCommentsOutput(comments.stream()
                .map(comment -> createCommunityCommentResponse(comment, postTitleByPostId, comment.getCreatedAt()))
                .toList());
    }

    public UserProfileCommunityCommentsOutput buildLikedComments(User user, String currentHandle) {
        // 커뮤니티 활동 비공개면 빈 좋아요 댓글 목록 반환
        if (!canShowCommunityActivity(user, currentHandle)) {
            return new UserProfileCommunityCommentsOutput(List.of());
        }

        // 좋아요한 댓글과 관련 게시글 제목 조회
        List<CommunityCommentLike> likedComments = communityCommentLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(user.getHandle());
        Map<Long, CommunityComment> commentById = communityCommentRepository.findAllByCommentIdIn(likedComments.stream()
                        .map(CommunityCommentLike::getId)
                        .map(commentLikeId -> commentLikeId.getCommentId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityComment::getCommentId, comment -> comment));
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(commentById.values().stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        // 좋아요한 댓글 응답 변환
        return new UserProfileCommunityCommentsOutput(likedComments.stream()
                .map(commentLike -> createLikedCommunityCommentResponse(commentLike, commentById, postTitleByPostId))
                .flatMap(Optional::stream)
                .toList());
    }

    public UserProfileCommunityActivitiesOutput buildCommunityActivities(User user,
                                                                        String currentHandle,
                                                                        int requestedPage,
                                                                        Integer requestedPageSize) {
        // 공개 여부를 반영한 커뮤니티 활동 페이지 생성
        if (!canShowCommunityActivity(user, currentHandle)) {
            return createCommunityActivitiesPage(List.of(), requestedPage, requestedPageSize);
        }

        return createCommunityActivitiesPage(createCommunityActivityResponses(user.getHandle()), requestedPage, requestedPageSize);
    }

    public UserProfileSummaryOutput updateProfile(User user, UserProfileUpdateInput input) {
        // 소개글, 기본 설정 수정
        user.changeProfile(
                normalizeBio(input.getBio()),
                normalizeProfileImageUrl(input.getProfileImageUrl()),
                normalizeBackgroundImageUrl(input.getBackgroundImageUrl()),
                input.getDefaultDbms(),
                input.isSqlPublic(),
                input.isExecutionPercentilePublic(),
                input.isSolvedRecordsPublic(),
                input.isSolvedProblemCountPublic(),
                input.isCommunityActivityPublic()
        );

        // 프로필 링크를 입력값으로 교체 후 요약 응답 생성
        replaceExternalLinks(user.getHandle(), input.getLinks());
        return buildUserProfileSummary(user, true);
    }

    private List<UserProfileCommunityActivityOutput> createCommunityActivityResponses(String handle) {
        // 작성, 좋아요, 댓글 활동을 하나의 목록으로 병합
        List<UserProfileCommunityActivityOutput> activities = new ArrayList<>();
        activities.addAll(createAuthoredPostActivities(handle));
        activities.addAll(createLikedPostActivities(handle));
        activities.addAll(createAuthoredCommentActivities(handle));
        activities.addAll(createLikedCommentActivities(handle));

        return activities.stream()
                .sorted(Comparator.comparing(UserProfileCommunityActivityOutput::getHappenedAt).reversed())
                .toList();
    }

    private List<UserProfileCommunityActivityOutput> createAuthoredPostActivities(String handle) {
        // 작성한 게시글 활동 목록 생성
        return communityPostRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                .map(post -> new UserProfileCommunityActivityOutput(
                        "post",
                        CommunityPostIdPolicy.format(post.getPostId()),
                        post.getTitle(),
                        null,
                        createCommunityExcerpt(post.getPlainTextSummary()),
                        post.getCreatedAt()
                ))
                .toList();
    }

    private List<UserProfileCommunityActivityOutput> createLikedPostActivities(String handle) {
        // 좋아요한 게시글 활동 목록 생성
        List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(handle);
        Map<Long, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(likedPosts.stream()
                        .map(CommunityPostLike::getId)
                        .map(postLikeId -> postLikeId.getPostId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));

        return likedPosts.stream()
                .map(postLike -> createLikedPostActivity(postLike, postById))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<UserProfileCommunityActivityOutput> createLikedPostActivity(CommunityPostLike postLike,
                                                                                 Map<Long, CommunityPost> postById) {
        // 좋아요한 게시글 활동 항목 생성
        return Optional.ofNullable(postById.get(postLike.getId().getPostId()))
                .map(post -> new UserProfileCommunityActivityOutput(
                        "likedPost",
                        CommunityPostIdPolicy.format(post.getPostId()),
                        post.getTitle(),
                        null,
                        "",
                        postLike.getCreatedAt()
                ));
    }

    private List<UserProfileCommunityActivityOutput> createAuthoredCommentActivities(String handle) {
        // 작성한 댓글 활동 목록 생성
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(handle);
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        return comments.stream()
                .map(comment -> createCommentActivity("comment", comment, postTitleByPostId, comment.getCreatedAt()))
                .toList();
    }

    private List<UserProfileCommunityActivityOutput> createLikedCommentActivities(String handle) {
        // 좋아요한 댓글 활동 목록 생성
        List<CommunityCommentLike> likedComments = communityCommentLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(handle);
        Map<Long, CommunityComment> commentById = communityCommentRepository.findAllByCommentIdIn(likedComments.stream()
                        .map(CommunityCommentLike::getId)
                        .map(commentLikeId -> commentLikeId.getCommentId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityComment::getCommentId, comment -> comment));
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(commentById.values().stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        return likedComments.stream()
                .map(commentLike -> createLikedCommentActivity(commentLike, commentById, postTitleByPostId))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<UserProfileCommunityActivityOutput> createLikedCommentActivity(CommunityCommentLike commentLike,
                                                                                    Map<Long, CommunityComment> commentById,
                                                                                    Map<Long, String> postTitleByPostId) {
        // 좋아요한 댓글 활동 항목 생성
        return Optional.ofNullable(commentById.get(commentLike.getId().getCommentId()))
                .map(comment -> createCommentActivity("likedComment", comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private UserProfileCommunityActivityOutput createCommentActivity(String activityType,
                                                                     CommunityComment comment,
                                                                     Map<Long, String> postTitleByPostId,
                                                                     java.time.LocalDateTime happenedAt) {
        // 댓글 기반 커뮤니티 활동 항목 생성
        return new UserProfileCommunityActivityOutput(
                activityType,
                CommunityPostIdPolicy.format(comment.getPostId()),
                postTitleByPostId.getOrDefault(comment.getPostId(), CommunityPostIdPolicy.format(comment.getPostId())),
                comment.getCommentId(),
                comment.getContent(),
                happenedAt
        );
    }

    private UserProfileCommunityActivitiesOutput createCommunityActivitiesPage(List<UserProfileCommunityActivityOutput> activities,
                                                                              int requestedPage,
                                                                              Integer requestedPageSize) {
        // 커뮤니티 활동 목록을 요청 페이지로 분할
        int pageSize = normalizeCommunityActivityPageSize(requestedPageSize);
        int totalPages = Math.max(1, (int) Math.ceil(activities.size() / (double) pageSize));
        int currentPage = Math.min(totalPages, Math.max(1, requestedPage));
        int fromIndex = Math.min(activities.size(), (currentPage - 1) * pageSize);
        int toIndex = Math.min(activities.size(), fromIndex + pageSize);

        return new UserProfileCommunityActivitiesOutput(
                currentPage,
                pageSize,
                activities.size(),
                totalPages,
                activities.subList(fromIndex, toIndex)
        );
    }

    private int normalizeCommunityActivityPageSize(Integer requestedPageSize) {
        // 커뮤니티 활동 페이지 크기 정규화
        if (requestedPageSize == null) {
            return UserProfilePageConstant.DEFAULT_COMMUNITY_ACTIVITY_PAGE_SIZE;
        }

        return Math.min(UserProfilePageConstant.MAX_COMMUNITY_ACTIVITY_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

    private boolean canShowCommunityActivity(User user, String currentHandle) {
        // 본인 또는 공개 설정된 사용자만 커뮤니티 활동 조회 허용
        return user.getHandle().equals(currentHandle) || user.isCommunityActivityPublicEnabled();
    }

    private void syncSolvedStatistics(User user, List<ProblemSolveHistory> histories) {
        // 해결한 통계 동기화
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
        // 최고 해결한 목록 생성
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
        // 해결한 문제 번호 목록 생성
        return histories.stream()
                .map(ProblemSolveHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> createAttemptedProblemIds(List<ProblemSubmitHistory> histories) {
        // 제출한 문제 번호 목록 생성
        return histories.stream()
                .map(ProblemSubmitHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<UserProfileSubmissionActivityOutput> createSubmissionActivityOutputs(List<ProblemSubmitHistory> histories) {
        // 제출 일자별 횟수 목록 생성
        Map<String, Long> countByDate = new LinkedHashMap<>();

        histories.forEach(history -> {
            String submittedDate = history.getSubmittedAt().toLocalDate().toString();
            countByDate.put(submittedDate, countByDate.getOrDefault(submittedDate, 0L) + 1);
        });

        return countByDate.entrySet().stream()
                .map(entry -> new UserProfileSubmissionActivityOutput(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<UserProfileSolvedRecordOutput> createSolvedRecordResponses(List<ProblemSolveHistory> bestSolvedHistories) {
        // 풀이 기록 응답 목록 생성
        return bestSolvedHistories.stream()
                .map(history -> new UserProfileSolvedRecordOutput(
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
        // 평균 실행 백분위 계산
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
        // 실행 백분위 계산
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
        // 더 나은 기록 선택
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

    private List<UserProfileLinkOutput> createProfileLinkResponses(List<UserExternalLink> externalLinks) {
        // 프로필 링크 응답 목록 생성
        return externalLinks.stream()
                .map(link -> new UserProfileLinkOutput(link.getType(), link.getLink()))
                .toList();
    }

    private List<UserProfileCommunityPostOutput> createCommunityPostResponses(List<CommunityPost> posts) {
        // 커뮤니티 게시글 응답 목록 생성
        return posts.stream()
                .map(this::createCommunityPostResponse)
                .toList();
    }

    private UserProfileCommunityPostOutput createCommunityPostResponse(CommunityPost post) {
        // 커뮤니티 게시글 응답 생성
        return new UserProfileCommunityPostOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(),
                createCommunityExcerpt(post.getPlainTextSummary()),
                communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(post.getPostId()).stream()
                        .map(CommunityPostTag::getTag)
                        .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount()
        );
    }

    private Optional<UserProfileCommunityPostOutput> createLikedCommunityPostResponse(CommunityPostLike postLike, Map<Long, CommunityPost> postById) {
        // 좋아요한 커뮤니티 게시글 응답 생성
        CommunityPost post = postById.get(postLike.getId().getPostId());

        if (post == null) {
            return Optional.empty();
        }

        return Optional.of(new UserProfileCommunityPostOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(),
                createCommunityExcerpt(post.getPlainTextSummary()),
                communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(post.getPostId()).stream()
                        .map(CommunityPostTag::getTag)
                        .toList(),
                postLike.getCreatedAt(),
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount()
        ));
    }

    private Optional<UserProfileCommunityCommentOutput> createLikedCommunityCommentResponse(CommunityCommentLike commentLike,
                                                                                            Map<Long, CommunityComment> commentById,
                                                                                            Map<Long, String> postTitleByPostId) {
        CommunityComment comment = commentById.get(commentLike.getId().getCommentId());

        if (comment == null) {
            return Optional.empty();
        }

        return Optional.of(createCommunityCommentResponse(comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private UserProfileCommunityCommentOutput createCommunityCommentResponse(CommunityComment comment,
                                                                             Map<Long, String> postTitleByPostId,
                                                                             java.time.LocalDateTime actedAt) {
        return new UserProfileCommunityCommentOutput(
                comment.getCommentId(),
                CommunityPostIdPolicy.format(comment.getPostId()),
                postTitleByPostId.getOrDefault(comment.getPostId(), CommunityPostIdPolicy.format(comment.getPostId())),
                comment.getContent(),
                actedAt,
                comment.getParentCommentId() != null
        );
    }

    private Map<Long, String> createPostTitleByPostId(List<Long> postIds) {
        // 게시글 번호별 게시글 제목 생성
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return communityPostRepository.findAllByPostIdIn(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, CommunityPost::getTitle));
    }

    private String createCommunityExcerpt(String contentText) {
        // 커뮤니티 요약문 생성
        if (contentText == null || contentText.isBlank()) {
            return "";
        }

        String normalizedContentText = contentText.trim();
        return normalizedContentText.length() > 120
                ? normalizedContentText.substring(0, 120).trim() + "..."
                : normalizedContentText;
    }

    private void replaceExternalLinks(String handle, List<UserProfileLinkInput> links) {
        // 외부 링크 목록 교체
        List<UserExternalLink> normalizedExternalLinks = normalizeExternalLinks(handle, links);

        // 사용자 링크를 최신 입력값으로 전체 교체
        userExternalLinkRepository.deleteAllByIdHandle(handle);

        if (normalizedExternalLinks.isEmpty()) {
            return;
        }

        userExternalLinkRepository.saveAll(normalizedExternalLinks);
    }

    private List<UserExternalLink> normalizeExternalLinks(String handle, List<UserProfileLinkInput> links) {
        // 외부 링크 목록 정규화
        Map<String, UserExternalLink> externalLinkByKey = new LinkedHashMap<>();

        // 공백, 중복 링크를 제거하고 저장용 엔티티 구성
        for (UserProfileLinkInput link : links) {
            String type = link.getType().trim();
            String value = link.getValue().trim();

            if (type.isEmpty() || value.isEmpty()) {
                continue;
            }

            externalLinkByKey.putIfAbsent(type + "|" + value, UserExternalLink.create(handle, type, value));
        }

        return new ArrayList<>(externalLinkByKey.values());
    }

    private String normalizeBio(String bio) {
        // Bio 정규화
        return bio != null ? bio.trim() : "";
    }

    private String normalizeProfileImageUrl(String profileImageUrl) {
        // 프로필 이미지 URL 정규화
        return profileImageUrl != null ? profileImageUrl.trim() : "";
    }

    private String normalizeBackgroundImageUrl(String backgroundImageUrl) {
        // 프로필 배경 이미지 URL 정규화
        return backgroundImageUrl != null ? backgroundImageUrl.trim() : "";
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        // 요청 DBMS 값을 내부 유형으로 맞춤
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    @Value
    @Accessors(fluent = true)
    private static class UserSolvedHistoryKey {
        // 사용자 해결한 기록 키 처리
        String problemId;
        DbmsType dbmsType;
    }

}
