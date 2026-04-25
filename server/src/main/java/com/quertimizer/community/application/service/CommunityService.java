package com.quertimizer.community.application.service;

import com.quertimizer.alarm.application.service.AlarmService;
import com.quertimizer.alarm.domain.model.CommunityPostLikeAlarm;
import com.quertimizer.alarm.domain.model.CommunityPostCommentAlarm;
import com.quertimizer.alarm.domain.model.CommunityCommentReplyAlarm;
import com.quertimizer.alarm.domain.model.CommunityCommentLikeAlarm;
import com.quertimizer.community.application.input.CommunityCommentInput;
import com.quertimizer.community.application.input.CommunityPostInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.output.CommunityPostDetailOutput;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.CommunityCommentLikeId;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.CommunityPostLikeId;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
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
public class CommunityService {

    private static final int COMMUNITY_PAGE_SIZE = 10;
    private static final int COMMUNITY_POST_CONTENT_MAX_BYTES = 500_000;

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunitySearchService communitySearchService;
    private final AlarmService alarmService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CommunityPostPageOutput getPosts(int requestedPage, String searchKeyword, String tag, String category, String sortKey) {
        // 게시글 목록 페이지를 조회
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        // 게시글 목록 검색, 필터, 정렬, 페이징
        return communitySearchService.searchPosts(
                requestedPage,
                COMMUNITY_PAGE_SIZE,
                searchKeyword,
                tag,
                category,
                sortKey,
                posts,
                tagsByPostId
        );
    }

    public Optional<CommunityPostDetailOutput> getPostDetail(Long postId, String currentHandle) {
        // 게시글 상세를 조회
        return communityPostRepository.findById(postId)
                .map(post -> {
                    // 상세 조회 시 조회수 증가
                    post.increaseViewCount();

                    List<String> tags = createTags(postId);
                    List<CommunityComment> comments = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId);
                    Map<Long, Boolean> likedCommentById = createLikedCommentById(comments, currentHandle);
                    boolean likedByCurrentUser = isPostLiked(postId, currentHandle);

                    CommunityPostDetailOutput detailResponse = new CommunityPostDetailOutput(
                            CommunityPostIdPolicy.format(post.getPostId()),
                            post.getTitle(),
                            post.getHandle(),
                            post.getContentJson(),
                            createImageIds(post.getImageIds()),
                            tags,
                            resolveCategory(post),
                            post.getCreatedAt(),
                            post.getUpdatedAt(),
                            post.getViewCount(),
                            post.getLikeCount(),
                            post.getCommentCount(),
                            likedByCurrentUser,
                            currentHandle != null && currentHandle.equals(post.getHandle()),
                            createCommentTree(comments, likedCommentById)
                    );

                    // 조회수 변경 후 검색 인덱스 반영
                    communitySearchService.syncPost(post, tags);
                    return detailResponse;
                });
    }

    public Long createPost(String handle, CommunityPostInput input) {
        // 게시글을 생성
        String normalizedTitle = input.getTitle().trim();
        String normalizedContentJson = normalizeContentJson(input.getContentJson());
        validateContentJson(normalizedContentJson);
        String normalizedPlainTextSummary = normalizePlainTextSummary(input.getPlainTextSummary());
        String normalizedImageIds = normalizeImageIds(input.getImageIds());
        List<String> normalizedTags = normalizeTags(input.getTags());
        String normalizedCategory = normalizePostCategory(input.getCategory());
        Long nextPostId = communityPostRepository.findTopPostId()
                .map(postId -> postId + 1)
                .orElse(1L);

        // 게시글 저장 후 태그와 검색 인덱스 동기화
        CommunityPost post = communityPostRepository.save(
                CommunityPost.create(nextPostId, handle, normalizedTitle, normalizedContentJson, normalizedPlainTextSummary, normalizedImageIds, normalizedCategory)
        );
        replaceTags(post.getPostId(), normalizedTags);
        communitySearchService.syncPost(post, normalizedTags);
        return post.getPostId();
    }

    public Optional<Long> updatePost(Long postId, String handle, CommunityPostInput input) {
        // 게시글을 수정
        return communityPostRepository.findById(postId)
                .filter(post -> post.getHandle().equals(handle))
                .map(post -> {
                    String normalizedTitle = input.getTitle().trim();
                    String normalizedContentJson = normalizeContentJson(input.getContentJson());
                    validateContentJson(normalizedContentJson);
                    String normalizedPlainTextSummary = normalizePlainTextSummary(input.getPlainTextSummary());
                    String normalizedImageIds = normalizeImageIds(input.getImageIds());
                    List<String> normalizedTags = normalizeTags(input.getTags());
                    String normalizedCategory = normalizePostCategory(input.getCategory());

                    // 게시글 본문, 태그, 검색 인덱스 갱신
                    post.changeContent(normalizedTitle, normalizedContentJson, normalizedPlainTextSummary, normalizedImageIds, normalizedCategory);
                    replaceTags(postId, normalizedTags);
                    communitySearchService.syncPost(post, normalizedTags);
                    return postId;
                });
    }

    public boolean deletePost(Long postId, String handle) {
        // 게시글을 삭제
        Optional<CommunityPost> post = communityPostRepository.findById(postId)
                .filter(currentPost -> currentPost.getHandle().equals(handle));

        if (post.isEmpty()) {
            return false;
        }

        // 게시글 삭제 시 댓글, 좋아요, 태그까지 함께 제거
        List<Long> commentIds = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommunityComment::getCommentId)
                .toList();

        if (!commentIds.isEmpty()) {
            communityCommentLikeRepository.deleteAllByIdCommentIdIn(commentIds);
        }

        communityCommentRepository.deleteAllByPostId(postId);
        communityPostLikeRepository.deleteAllByIdPostId(postId);
        communityPostTagRepository.deleteAllByPostId(postId);
        communityPostRepository.delete(post.get());
        communitySearchService.deletePost(postId);
        return true;
    }

    public Optional<CommunityReactionOutput> togglePostLike(Long postId, String handle) {
        // 게시글 좋아요를 토글
        return communityPostRepository.findById(postId)
                .map(post -> {
                    CommunityPostLikeId postLikeId = new CommunityPostLikeId(postId, handle);

                    // 게시글 좋아요 토글 후 카운트, 검색 인덱스 갱신
                    if (communityPostLikeRepository.existsById(postLikeId)) {
                        communityPostLikeRepository.deleteById(postLikeId);
                        post.decreaseLikeCount();
                        communitySearchService.syncPost(post, createTags(postId));
                        return new CommunityReactionOutput(false, post.getLikeCount());
                    }

                    communityPostLikeRepository.save(CommunityPostLike.create(postId, handle));
                    post.increaseLikeCount();
                    communitySearchService.syncPost(post, createTags(postId));
                    publishPostLikeAlarm(post, handle);
                    return new CommunityReactionOutput(true, post.getLikeCount());
                });
    }

    public Optional<CommunityCommentOutput> addComment(Long postId, String handle, CommunityCommentInput input) {
        // 댓글을 추가
        return communityPostRepository.findById(postId)
                .map(post -> {
                    Optional<CommunityComment> parentComment = Optional.ofNullable(input.getParentCommentId())
                            .flatMap(communityCommentRepository::findById)
                            .filter(currentComment -> currentComment.getPostId().equals(postId));

                    // 댓글 저장 후 댓글 수, 검색 인덱스 갱신
                    CommunityComment comment = communityCommentRepository.save(
                            CommunityComment.create(
                                    postId,
                                    handle,
                                    input.getParentCommentId(),
                                    input.getContent().trim()
                            )
                    );
                    post.increaseCommentCount();
                    communitySearchService.syncPost(post, createTags(postId));
                    publishCommentAlarms(post, comment, parentComment, handle);
                    return new CommunityCommentOutput(
                            comment.getCommentId(),
                            comment.getHandle(),
                            comment.getContent(),
                            comment.getCreatedAt(),
                            comment.getLikeCount(),
                            false,
                            List.of()
                    );
                });
    }

    public Optional<CommunityReactionOutput> toggleCommentLike(Long commentId, String handle) {
        // 댓글 좋아요를 토글
        return communityCommentRepository.findById(commentId)
                .map(comment -> {
                    CommunityCommentLikeId commentLikeId = new CommunityCommentLikeId(commentId, handle);

                    // 댓글 좋아요 토글 후 카운트 갱신
                    if (communityCommentLikeRepository.existsById(commentLikeId)) {
                        communityCommentLikeRepository.deleteById(commentLikeId);
                        comment.decreaseLikeCount();
                        return new CommunityReactionOutput(false, comment.getLikeCount());
                    }

                    communityCommentLikeRepository.save(CommunityCommentLike.create(commentId, handle));
                    comment.increaseLikeCount();
                    publishCommentLikeAlarm(comment, handle);
                    return new CommunityReactionOutput(true, comment.getLikeCount());
                });
    }

    @Transactional(readOnly = true)
    public List<CommunityTagSuggestionOutput> getTagSuggestions(String query) {
        // 태그 자동완성 목록을 조회
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        Map<String, Long> usageCountByTag = new LinkedHashMap<>();

        // 기존 게시글 태그 기준 자동완성 목록 구성
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByTagContainingIgnoreCaseOrderByTagAsc(query.trim())) {
            usageCountByTag.merge(postTag.getTag(), 1L, Long::sum);
        }

        return usageCountByTag.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(entry -> new CommunityTagSuggestionOutput(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void publishPostLikeAlarm(CommunityPost post, String actorHandle) {
        // 게시글 좋아요 알람 발행
        if (post.getHandle().equals(actorHandle)) {
            return;
        }

        alarmService.publish(new CommunityPostLikeAlarm(
                post.getHandle(),
                actorHandle,
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle()
        ));
    }

    private void publishCommentAlarms(CommunityPost post,
                                      CommunityComment comment,
                                      Optional<CommunityComment> parentComment,
                                      String actorHandle) {
        boolean isReplyAlarmDelivered = false;

        if (parentComment.isPresent() && !parentComment.get().getHandle().equals(actorHandle)) {
            alarmService.publish(new CommunityCommentReplyAlarm(
                    parentComment.get().getHandle(),
                    actorHandle,
                    CommunityPostIdPolicy.format(post.getPostId()),
                    comment.getContent(),
                    comment.getCommentId()
            ));
            isReplyAlarmDelivered = true;
        }

        if (post.getHandle().equals(actorHandle)) {
            return;
        }

        if (isReplyAlarmDelivered && parentComment.map(currentComment -> currentComment.getHandle().equals(post.getHandle())).orElse(false)) {
            return;
        }

        alarmService.publish(new CommunityPostCommentAlarm(
                post.getHandle(),
                actorHandle,
                CommunityPostIdPolicy.format(post.getPostId()),
                comment.getContent(),
                comment.getCommentId()
        ));
    }

    private void publishCommentLikeAlarm(CommunityComment comment, String actorHandle) {
        // 댓글 좋아요 알람 발행
        if (comment.getHandle().equals(actorHandle)) {
            return;
        }

        alarmService.publish(new CommunityCommentLikeAlarm(
                comment.getHandle(),
                actorHandle,
                CommunityPostIdPolicy.format(comment.getPostId()),
                comment.getContent(),
                comment.getCommentId()
        ));
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 번호별 태그 목록 생성
        Map<Long, List<String>> tagsByPostId = new HashMap<>();

        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        // 게시글별 태그 목록 구성
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>())
                    .add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private List<String> createTags(Long postId) {
        // 태그 목록 생성
        return communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(postId).stream()
                .map(CommunityPostTag::getTag)
                .toList();
    }

    private List<String> normalizeTags(List<String> tags) {
        // 태그 목록 정규화
        Map<String, String> tagByNormalizedValue = new LinkedHashMap<>();

        // 공백, 중복 태그 제거 후 최대 10개 유지
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }

            String normalizedTag = tag.trim();
            tagByNormalizedValue.putIfAbsent(normalizedTag.toLowerCase(), normalizedTag);

            if (tagByNormalizedValue.size() == 10) {
                break;
            }
        }

        return new ArrayList<>(tagByNormalizedValue.values());
    }

    private String normalizePostCategory(String category) {
        // 게시글 구분 정규화
        if (!StringUtils.hasText(category)) {
            return "discussion";
        }

        String normalizedCategory = category.trim().toLowerCase();
        return switch (normalizedCategory) {
            case "notice", "question" -> normalizedCategory;
            default -> "discussion";
        };
    }

    private String resolveCategory(CommunityPost post) {
        // 게시글 구분 조회
        return normalizePostCategory(post.getCategory());
    }

    private void replaceTags(Long postId, List<String> tags) {
        // 태그 목록 교체
        communityPostTagRepository.deleteAllByPostId(postId);

        if (tags.isEmpty()) {
            return;
        }

        List<CommunityPostTag> postTags = new ArrayList<>();
        for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
            postTags.add(CommunityPostTag.create(postId, tags.get(tagIndex), tagIndex));
        }

        communityPostTagRepository.saveAll(postTags);
    }

    private String normalizeContentJson(String contentJson) {
        // 본문 JSON 정규화
        return StringUtils.hasText(contentJson) ? contentJson.trim() : "";
    }

    private void validateContentJson(String contentJson) {
        // 본문 JSON 형식과 Byte 길이를 검증
        if (contentJson.getBytes(StandardCharsets.UTF_8).length > COMMUNITY_POST_CONTENT_MAX_BYTES) {
            throw new BusinessException("본문은 최대 500000 Byte까지 입력할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            objectMapper.readTree(contentJson);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("본문 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizePlainTextSummary(String plainTextSummary) {
        // 본문 요약 텍스트 정규화
        return StringUtils.hasText(plainTextSummary)
                ? plainTextSummary.trim()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                : "";
    }

    private String normalizeImageIds(List<String> imageIds) {
        // 이미지 번호 목록 정규화
        if (imageIds == null || imageIds.isEmpty()) {
            return "";
        }

        return imageIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(100)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<String> createImageIds(String imageIds) {
        // 이미지 번호 목록 생성
        if (!StringUtils.hasText(imageIds)) {
            return List.of();
        }

        return List.of(imageIds.split(",")).stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean isPostLiked(Long postId, String currentHandle) {
        // 게시글 좋아요한 여부 확인
        if (currentHandle == null) {
            return false;
        }

        return communityPostLikeRepository.existsById(new CommunityPostLikeId(postId, currentHandle));
    }

    private Map<Long, Boolean> createLikedCommentById(List<CommunityComment> comments, String currentHandle) {
        // 번호별 좋아요한 댓글 생성
        Map<Long, Boolean> likedCommentById = new HashMap<>();

        if (currentHandle == null) {
            return likedCommentById;
        }

        // 현재 사용자 댓글 좋아요 여부 map 구성
        for (CommunityComment comment : comments) {
            likedCommentById.put(
                    comment.getCommentId(),
                    communityCommentLikeRepository.existsById(new CommunityCommentLikeId(comment.getCommentId(), currentHandle))
            );
        }

        return likedCommentById;
    }

    private List<CommunityCommentOutput> createCommentTree(List<CommunityComment> comments, Map<Long, Boolean> likedCommentById) {
        // 댓글 트리 생성
        Map<Long, List<CommunityComment>> childCommentsByParentId = new HashMap<>();
        List<CommunityComment> rootComments = new ArrayList<>();

        // 루트 댓글과 대댓글을 분리해 트리 구성 준비
        for (CommunityComment comment : comments) {
            if (comment.getParentCommentId() == null) {
                rootComments.add(comment);
                continue;
            }

            childCommentsByParentId.computeIfAbsent(comment.getParentCommentId(), key -> new ArrayList<>())
                    .add(comment);
        }

        rootComments.sort(Comparator.comparing(CommunityComment::getCreatedAt));
        return rootComments.stream()
                .map(comment -> createCommentResponse(comment, childCommentsByParentId, likedCommentById))
                .toList();
    }

    private CommunityCommentOutput createCommentResponse(CommunityComment comment,
                                                         Map<Long, List<CommunityComment>> childCommentsByParentId,
                                                         Map<Long, Boolean> likedCommentById) {
        List<CommunityCommentOutput> replies = childCommentsByParentId.getOrDefault(comment.getCommentId(), List.of()).stream()
                .sorted(Comparator.comparing(CommunityComment::getCreatedAt))
                .map(reply -> createCommentResponse(reply, childCommentsByParentId, likedCommentById))
                .toList();

        return new CommunityCommentOutput(
                comment.getCommentId(),
                comment.getHandle(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                likedCommentById.getOrDefault(comment.getCommentId(), false),
                replies
        );
    }

}
