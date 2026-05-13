package com.quertimizer.user.adapter.out.community;

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
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.output.UserProfileCommunityActivityOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.port.out.UserProfileCommunityPort;
import com.quertimizer.user.domain.model.UserProfileCommunityCounts;
import com.quertimizer.user.domain.model.UserProfilePageConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("userCommunityGateway")
@RequiredArgsConstructor
public class CommunityGateway implements UserProfileCommunityPort {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostTagRepositoryPort communityPostTagRepository;
    private final CommunityCommentRepositoryPort communityCommentRepository;
    private final CommunityCommentLikeRepositoryPort communityCommentLikeRepository;
    private final CommunityPostLikeRepositoryPort communityPostLikeRepository;

    @Override
    public UserProfileCommunityCounts getCommunityCounts(String handle) {
        // 커뮤니티 활동 수 집계
        return new UserProfileCommunityCounts(
                communityPostRepository.countByHandle(handle),
                communityPostLikeRepository.countByIdHandle(handle),
                communityCommentRepository.countByHandle(handle)
        );
    }

    @Override
    public UserProfileCommunityPostsOutput getAuthoredPosts(String handle) {
        // 작성 게시글 목록 조회 후 응답 변환
        return new UserProfileCommunityPostsOutput(
                communityPostRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                        .map(this::createCommunityPostResponse)
                        .toList()
        );
    }

    @Override
    public UserProfileCommunityPostsOutput getLikedPosts(String handle) {
        // 좋아요한 게시글 목록 조회
        List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(handle);
        Map<Long, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(likedPosts.stream()
                        .map(CommunityPostLike::getId)
                        .map(postLikeId -> postLikeId.getPostId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));

        // 좋아요한 게시글 응답 변환
        return new UserProfileCommunityPostsOutput(likedPosts.stream()
                .map(postLike -> createLikedCommunityPostResponse(postLike, postById))
                .flatMap(Optional::stream)
                .toList());
    }

    @Override
    public UserProfileCommunityCommentsOutput getAuthoredComments(String handle) {
        // 작성 댓글 목록과 게시글 제목 조회
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(handle);
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        // 작성 댓글 응답 변환
        return new UserProfileCommunityCommentsOutput(comments.stream()
                .map(comment -> createCommunityCommentResponse(comment, postTitleByPostId, comment.getCreatedAt()))
                .toList());
    }

    @Override
    public UserProfileCommunityCommentsOutput getLikedComments(String handle) {
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

        // 좋아요한 댓글 응답 변환
        return new UserProfileCommunityCommentsOutput(likedComments.stream()
                .map(commentLike -> createLikedCommunityCommentResponse(commentLike, commentById, postTitleByPostId))
                .flatMap(Optional::stream)
                .toList());
    }

    @Override
    public UserProfileCommunityActivitiesOutput getActivities(String handle, int requestedPage, Integer requestedPageSize) {
        // 작성, 좋아요, 댓글 활동을 하나의 목록으로 병합
        List<UserProfileCommunityActivityOutput> activities = new ArrayList<>();
        activities.addAll(createAuthoredPostActivities(handle));
        activities.addAll(createLikedPostActivities(handle));
        activities.addAll(createAuthoredCommentActivities(handle));
        activities.addAll(createLikedCommentActivities(handle));

        // 최신 활동 순으로 페이지 반환
        return createCommunityActivitiesPage(activities.stream()
                .sorted(Comparator.comparing(UserProfileCommunityActivityOutput::getHappenedAt).reversed())
                .toList(), requestedPage, requestedPageSize);
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
        // 좋아요한 게시글과 관련 게시글 조회
        List<CommunityPostLike> likedPosts = communityPostLikeRepository.findAllByIdHandleOrderByCreatedAtDesc(handle);
        Map<Long, CommunityPost> postById = communityPostRepository.findAllByPostIdIn(likedPosts.stream()
                        .map(CommunityPostLike::getId)
                        .map(postLikeId -> postLikeId.getPostId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(CommunityPost::getPostId, post -> post));

        // 좋아요한 게시글 활동 변환
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
        // 작성한 댓글과 관련 게시글 제목 조회
        List<CommunityComment> comments = communityCommentRepository.findAllByHandleOrderByCreatedAtDesc(handle);
        Map<Long, String> postTitleByPostId = createPostTitleByPostId(comments.stream()
                .map(CommunityComment::getPostId)
                .distinct()
                .toList());

        // 작성한 댓글 활동 변환
        return comments.stream()
                .map(comment -> createCommentActivity("comment", comment, postTitleByPostId, comment.getCreatedAt()))
                .toList();
    }

    private List<UserProfileCommunityActivityOutput> createLikedCommentActivities(String handle) {
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

        // 좋아요한 댓글 활동 변환
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
                                                                     LocalDateTime happenedAt) {
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
                currentPage, pageSize, activities.size(), totalPages,
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

    private Optional<UserProfileCommunityPostOutput> createLikedCommunityPostResponse(CommunityPostLike postLike,
                                                                                      Map<Long, CommunityPost> postById) {
        // 좋아요한 커뮤니티 게시글 존재 여부 확인
        CommunityPost post = postById.get(postLike.getId().getPostId());
        if (post == null) {
            return Optional.empty();
        }

        // 좋아요한 커뮤니티 게시글 응답 생성
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
        // 좋아요한 댓글 존재 여부 확인
        CommunityComment comment = commentById.get(commentLike.getId().getCommentId());
        if (comment == null) {
            return Optional.empty();
        }

        return Optional.of(createCommunityCommentResponse(comment, postTitleByPostId, commentLike.getCreatedAt()));
    }

    private UserProfileCommunityCommentOutput createCommunityCommentResponse(CommunityComment comment,
                                                                             Map<Long, String> postTitleByPostId,
                                                                             LocalDateTime actedAt) {
        // 커뮤니티 댓글 응답 생성
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
        // 게시글 번호가 없으면 빈 제목 map 반환
        if (postIds.isEmpty()) {
            return Map.of();
        }

        // 게시글 번호별 제목 map 반환
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

}
