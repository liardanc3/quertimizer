package com.quertimizer.community.application.service;

import com.quertimizer.community.application.output.CommunityUserActivitiesOutput;
import com.quertimizer.community.application.output.CommunityUserActivityOutput;
import com.quertimizer.community.application.output.CommunityUserCommentOutput;
import com.quertimizer.community.application.output.CommunityUserCountsOutput;
import com.quertimizer.community.application.output.CommunityUserPostOutput;
import com.quertimizer.community.application.port.in.CommunityUserProfileQuery;
import com.quertimizer.community.application.port.out.CommunityCommentLikeRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityCommentRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostLikeRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.model.CommunityProfileActivityPageConstant;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityUserProfileQueryService implements CommunityUserProfileQuery {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostTagRepositoryPort communityPostTagRepository;
    private final CommunityCommentRepositoryPort communityCommentRepository;
    private final CommunityCommentLikeRepositoryPort communityCommentLikeRepository;
    private final CommunityPostLikeRepositoryPort communityPostLikeRepository;

    @Override
    public CommunityUserCountsOutput getCounts(String handle) {
        // 커뮤니티 활동 수 집계
        return new CommunityUserCountsOutput(
                communityPostRepository.countByHandle(handle),
                communityPostLikeRepository.countByIdHandle(handle),
                communityCommentRepository.countByHandle(handle)
        );
    }

    @Override
    public List<CommunityUserPostOutput> getAuthoredPosts(String handle) {
        // 작성 게시글 목록 조회 후 응답 변환
        return communityPostRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                .map(post -> createPostOutput(post, post.getCreatedAt()))
                .toList();
    }

    @Override
    public List<CommunityUserPostOutput> getLikedPosts(String handle) {
        // 좋아요한 게시글 목록과 원본 게시글 조회
        List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(handle);
        Map<Long, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(likedPosts.stream()
                        .map(CommunityPostLike::getId)
                        .map(postLikeId -> postLikeId.getPostId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));

        return likedPosts.stream()
                .map(postLike -> createLikedPostOutput(postLike, postById))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<CommunityUserCommentOutput> getAuthoredComments(String handle) {
        // 작성 댓글 목록과 게시글 제목 조회
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(handle);
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        return comments.stream()
                .map(comment -> createCommentOutput(comment, postTitleByPostId, comment.getCreatedAt()))
                .toList();
    }

    @Override
    public List<CommunityUserCommentOutput> getLikedComments(String handle) {
        // 좋아요한 댓글 목록과 관련 게시글 제목 조회
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
                .map(commentLike -> createLikedCommentOutput(commentLike, commentById, postTitleByPostId))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public CommunityUserActivitiesOutput getActivities(String handle, int requestedPage, Integer requestedPageSize) {
        // 작성, 좋아요, 댓글 활동을 하나의 목록으로 병합
        List<CommunityUserActivityOutput> activities = new ArrayList<>();
        activities.addAll(createAuthoredPostActivities(handle));
        activities.addAll(createLikedPostActivities(handle));
        activities.addAll(createAuthoredCommentActivities(handle));
        activities.addAll(createLikedCommentActivities(handle));

        // 최신 활동 순으로 페이지 반환
        return createActivitiesPage(activities.stream()
                .sorted(Comparator.comparing(CommunityUserActivityOutput::getHappenedAt).reversed())
                .toList(), requestedPage, requestedPageSize);
    }

    private Optional<CommunityUserPostOutput> createLikedPostOutput(CommunityPostLike postLike, Map<Long, CommunityPost> postById) {
        // 좋아요한 게시글 응답 생성
        return Optional.ofNullable(postById.get(postLike.getId().getPostId()))
                .map(post -> createPostOutput(post, postLike.getCreatedAt()));
    }

    private CommunityUserPostOutput createPostOutput(CommunityPost post, LocalDateTime actedAt) {
        // 커뮤니티 게시글 응답 생성
        return new CommunityUserPostOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(),
                createExcerpt(post.getPlainTextSummary()),
                communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(post.getPostId()).stream()
                        .map(CommunityPostTag::getTag)
                        .toList(),
                actedAt,
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount()
        );
    }

    private Optional<CommunityUserCommentOutput> createLikedCommentOutput(CommunityCommentLike commentLike,
                                                                          Map<Long, CommunityComment> commentById,
                                                                          Map<Long, String> postTitleByPostId) {
        // 좋아요한 댓글 응답 생성
        return Optional.ofNullable(commentById.get(commentLike.getId().getCommentId()))
                .map(comment -> createCommentOutput(comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private CommunityUserCommentOutput createCommentOutput(CommunityComment comment,
                                                           Map<Long, String> postTitleByPostId,
                                                           LocalDateTime actedAt) {
        // 커뮤니티 댓글 응답 생성
        return new CommunityUserCommentOutput(
                comment.getCommentId(),
                CommunityPostIdPolicy.format(comment.getPostId()),
                postTitleByPostId.getOrDefault(comment.getPostId(), CommunityPostIdPolicy.format(comment.getPostId())),
                comment.getContent(),
                actedAt,
                comment.getParentCommentId() != null
        );
    }

    private List<CommunityUserActivityOutput> createAuthoredPostActivities(String handle) {
        // 작성한 게시글 활동 목록 생성
        return communityPostRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                .map(post -> new CommunityUserActivityOutput(
                        "post",
                        CommunityPostIdPolicy.format(post.getPostId()),
                        post.getTitle(),
                        null,
                        createExcerpt(post.getPlainTextSummary()),
                        post.getCreatedAt()
                ))
                .toList();
    }

    private List<CommunityUserActivityOutput> createLikedPostActivities(String handle) {
        // 좋아요한 게시글과 관련 게시글 조회
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

    private Optional<CommunityUserActivityOutput> createLikedPostActivity(CommunityPostLike postLike,
                                                                          Map<Long, CommunityPost> postById) {
        // 좋아요한 게시글 활동 항목 생성
        return Optional.ofNullable(postById.get(postLike.getId().getPostId()))
                .map(post -> new CommunityUserActivityOutput(
                        "likedPost",
                        CommunityPostIdPolicy.format(post.getPostId()),
                        post.getTitle(),
                        null,
                        "",
                        postLike.getCreatedAt()
                ));
    }

    private List<CommunityUserActivityOutput> createAuthoredCommentActivities(String handle) {
        // 작성한 댓글과 관련 게시글 제목 조회
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(handle);
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        return comments.stream()
                .map(comment -> createCommentActivity("comment", comment, postTitleByPostId, comment.getCreatedAt()))
                .toList();
    }

    private List<CommunityUserActivityOutput> createLikedCommentActivities(String handle) {
        // 좋아요한 댓글과 관련 게시글 제목 조회
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

    private Optional<CommunityUserActivityOutput> createLikedCommentActivity(CommunityCommentLike commentLike,
                                                                             Map<Long, CommunityComment> commentById,
                                                                             Map<Long, String> postTitleByPostId) {
        // 좋아요한 댓글 활동 항목 생성
        return Optional.ofNullable(commentById.get(commentLike.getId().getCommentId()))
                .map(comment -> createCommentActivity("likedComment", comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private CommunityUserActivityOutput createCommentActivity(String activityType,
                                                              CommunityComment comment,
                                                              Map<Long, String> postTitleByPostId,
                                                              LocalDateTime happenedAt) {
        // 댓글 기반 커뮤니티 활동 항목 생성
        return new CommunityUserActivityOutput(
                activityType,
                CommunityPostIdPolicy.format(comment.getPostId()),
                postTitleByPostId.getOrDefault(comment.getPostId(), CommunityPostIdPolicy.format(comment.getPostId())),
                comment.getCommentId(),
                comment.getContent(),
                happenedAt
        );
    }

    private CommunityUserActivitiesOutput createActivitiesPage(List<CommunityUserActivityOutput> activities,
                                                               int requestedPage,
                                                               Integer requestedPageSize) {
        // 커뮤니티 활동 목록을 요청 페이지로 분할
        int pageSize = normalizePageSize(requestedPageSize);
        int totalPages = Math.max(1, (int) Math.ceil(activities.size() / (double) pageSize));
        int currentPage = Math.min(totalPages, Math.max(1, requestedPage));
        int fromIndex = Math.min(activities.size(), (currentPage - 1) * pageSize);
        int toIndex = Math.min(activities.size(), fromIndex + pageSize);

        return new CommunityUserActivitiesOutput(
                currentPage, pageSize, activities.size(), totalPages,
                activities.subList(fromIndex, toIndex)
        );
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 커뮤니티 활동 페이지 크기 정규화
        if (requestedPageSize == null) {
            return CommunityProfileActivityPageConstant.DEFAULT_PAGE_SIZE;
        }

        return Math.min(CommunityProfileActivityPageConstant.MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

    private Map<Long, String> createPostTitleByPostId(List<Long> postIds) {
        // 게시글 번호가 없으면 빈 제목 map 반환
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return communityPostRepository.findAllByPostIdIn(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, CommunityPost::getTitle));
    }

    private String createExcerpt(String contentText) {
        // 커뮤니티 요약문 생성
        if (contentText == null || contentText.isBlank()) {
            return "";
        }

        String normalizedContentText = contentText.trim();
        return normalizedContentText.length() > 120
                ? normalizedContentText.substring(0, 120).trim() + "..."
                : normalizedContentText;
    }
}
