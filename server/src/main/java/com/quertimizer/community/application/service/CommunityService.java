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
import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunitySearchService communitySearchService;
    private final AlarmService alarmService;

    @Transactional(readOnly = true)
    public CommunityPostPageOutput getPosts(int requestedPage, String searchKeyword, String tag, String category, String sortKey) {
        // 게시글 목록 페이지를 조회
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<String, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

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

    public Optional<CommunityPostDetailOutput> getPostDetail(String postId, String currentHandle) {
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
                            post.getPostId(),
                            post.getTitle(),
                            post.getHandle(),
                            post.getContentHtml(),
                            tags,
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

    public String createPost(String handle, CommunityPostInput input) {
        // 게시글을 생성
        String normalizedTitle = input.getTitle().trim();
        String normalizedContentHtml = normalizeContentHtml(input.getContentHtml());
        String normalizedContentText = extractPlainText(normalizedContentHtml);
        List<String> normalizedTags = normalizeTags(input.getTags());

        // 게시글 저장 후 태그와 검색 인덱스 동기화
        CommunityPost post = communityPostRepository.save(
                CommunityPost.create(handle, normalizedTitle, normalizedContentHtml, normalizedContentText)
        );
        replaceTags(post.getPostId(), normalizedTags);
        communitySearchService.syncPost(post, normalizedTags);
        return post.getPostId();
    }

    public Optional<String> updatePost(String postId, String handle, CommunityPostInput input) {
        // 게시글을 수정
        return communityPostRepository.findById(postId)
                .filter(post -> post.getHandle().equals(handle))
                .map(post -> {
                    String normalizedTitle = input.getTitle().trim();
                    String normalizedContentHtml = normalizeContentHtml(input.getContentHtml());
                    String normalizedContentText = extractPlainText(normalizedContentHtml);
                    List<String> normalizedTags = normalizeTags(input.getTags());

                    // 게시글 본문, 태그, 검색 인덱스 갱신
                    post.changeContent(normalizedTitle, normalizedContentHtml, normalizedContentText);
                    replaceTags(postId, normalizedTags);
                    communitySearchService.syncPost(post, normalizedTags);
                    return postId;
                });
    }

    public boolean deletePost(String postId, String handle) {
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

    public Optional<CommunityReactionOutput> togglePostLike(String postId, String handle) {
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

    public Optional<CommunityCommentOutput> addComment(String postId, String handle, CommunityCommentInput input) {
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

        alarmService.publish(new CommunityPostLikeAlarm(post.getHandle(), actorHandle, post.getPostId(), post.getTitle()));
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
                    post.getPostId(),
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
                post.getPostId(),
                comment.getContent(),
                comment.getCommentId()
        ));
    }

    private void publishCommentLikeAlarm(CommunityComment comment, String actorHandle) {
        // 댓글 좋아요 알람 발행
        if (comment.getHandle().equals(actorHandle)) {
            return;
        }

        alarmService.publish(new CommunityCommentLikeAlarm(comment.getHandle(), actorHandle, comment.getPostId(), comment.getContent(), comment.getCommentId()));
    }

    private Map<String, List<String>> createTagsByPostId(List<String> postIds) {
        // 게시글 번호별 태그 목록 생성
        Map<String, List<String>> tagsByPostId = new HashMap<>();

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

    private List<String> createTags(String postId) {
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

    private void replaceTags(String postId, List<String> tags) {
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

    private String normalizeContentHtml(String contentHtml) {
        // 본문 HTML 정규화
        return StringUtils.hasText(contentHtml) ? contentHtml.trim() : "";
    }

    private String extractPlainText(String contentHtml) {
        // 텍스트 추출
        return normalizeContentHtml(contentHtml)
                .replaceAll("(?i)<img[^>]*>", " ")
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</(p|div|li|h1|h2|h3|blockquote|figure|figcaption)>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isPostLiked(String postId, String currentHandle) {
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
