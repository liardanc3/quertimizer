package com.quertimizer.community.application.service;

import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.port.out.CommunityAlarmPort;
import com.quertimizer.community.application.port.out.CommunityCommentLikeRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostLikeRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import com.quertimizer.community.domain.model.CommunityPostConstant;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
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

    private final CommunityPostTagRepositoryPort communityPostTagRepository;
    private final CommunityPostLikeRepositoryPort communityPostLikeRepository;
    private final CommunityCommentLikeRepositoryPort communityCommentLikeRepository;
    private final CommunityAlarmPort communityAlarmPort;

    public Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 번호별 태그 저장소 준비
        Map<Long, List<String>> tagsByPostId = new HashMap<>();

        // 조회 대상 게시글 번호 없으면 빈 결과 반환
        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        // 게시글별 태그 목록 구성
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>()).add(postTag.getTag());
        }

        return tagsByPostId;
    }

    public List<String> createTags(Long postId) {
        // 게시글 번호 기준 태그 목록 조회
        return communityPostTagRepository.findAllByPostIdOrderByTagOrderAsc(postId).stream()
                .map(CommunityPostTag::getTag)
                .toList();
    }

    public void replaceTags(Long postId, List<String> tags) {
        // 기존 태그 삭제
        communityPostTagRepository.deleteAllByPostId(postId);

        // 신규 태그 없으면 종료
        if (tags.isEmpty()) {
            return;
        }

        // 태그 순서와 함께 신규 태그 저장
        List<CommunityPostTag> postTags = new ArrayList<>();
        for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
            postTags.add(CommunityPostTag.create(postId, tags.get(tagIndex), tagIndex));
        }
        communityPostTagRepository.saveAll(postTags);
    }

    public List<String> normalizeTags(List<String> tags) {
        // 태그 정규화 결과 저장소 준비
        Map<String, String> tagByNormalizedValue = new LinkedHashMap<>();

        // 공백, 중복 태그 제거 후 최대 태그 수 유지
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }

            String normalizedTag = tag.trim();
            tagByNormalizedValue.putIfAbsent(normalizedTag.toLowerCase(), normalizedTag);

            if (tagByNormalizedValue.size() == CommunityPostConstant.MAX_TAG_COUNT) {
                break;
            }
        }

        return new ArrayList<>(tagByNormalizedValue.values());
    }

    public String normalizePostCategory(String category) {
        // 게시글 구분 없으면 기본 구분 반환
        if (!StringUtils.hasText(category)) {
            return CommunityPostConstant.DEFAULT_CATEGORY;
        }

        // 허용 구분만 유지하고 나머지는 기본 구분 대체
        String normalizedCategory = category.trim().toLowerCase();
        return switch (normalizedCategory) {
            case "notice", "question" -> normalizedCategory;
            default -> CommunityPostConstant.DEFAULT_CATEGORY;
        };
    }

    public String resolveCategory(CommunityPost post) {
        // 게시글 구분 정규화
        return normalizePostCategory(post.getCategory());
    }

    public String normalizeContentJson(String contentJson) {
        // 본문 JSON 정규화
        return StringUtils.hasText(contentJson) ? contentJson.trim() : "";
    }

    public String normalizePlainTextSummary(String plainTextSummary) {
        // 본문 요약 텍스트 정규화
        return StringUtils.hasText(plainTextSummary)
                ? plainTextSummary.trim()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                : "";
    }

    public String normalizeImageIds(List<String> imageIds) {
        // 이미지 번호 목록 없으면 빈 문자열 반환
        if (imageIds == null || imageIds.isEmpty()) {
            return "";
        }

        // 이미지 번호 목록 공백 제거와 중복 제거
        return imageIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(CommunityPostConstant.MAX_IMAGE_ID_COUNT)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    public List<String> createImageIds(String imageIds) {
        // 이미지 번호 문자열 없으면 빈 목록 반환
        if (!StringUtils.hasText(imageIds)) {
            return List.of();
        }

        // 이미지 번호 문자열 분리
        return List.of(imageIds.split(",")).stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    public boolean isPostLiked(Long postId, String currentHandle) {
        // 현재 사용자 없으면 좋아요 여부 false 반환
        if (currentHandle == null) {
            return false;
        }

        // 게시글 좋아요 여부 조회
        return communityPostLikeRepository.existsById(new CommunityPostLikeId(postId, currentHandle));
    }

    public Map<Long, Boolean> createLikedCommentById(List<CommunityComment> comments, String currentHandle) {
        // 댓글 좋아요 여부 저장소 준비
        Map<Long, Boolean> likedCommentById = new HashMap<>();

        // 현재 사용자 없으면 빈 결과 반환
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

    public List<CommunityCommentOutput> createCommentTree(List<CommunityComment> comments, Map<Long, Boolean> likedCommentById) {
        // 댓글 트리 구성 저장소 준비
        Map<Long, List<CommunityComment>> childCommentsByParentId = new HashMap<>();
        List<CommunityComment> rootComments = new ArrayList<>();

        // 루트 댓글과 대댓글 분리
        for (CommunityComment comment : comments) {
            if (comment.getParentCommentId() == null) {
                rootComments.add(comment);
                continue;
            }

            childCommentsByParentId.computeIfAbsent(comment.getParentCommentId(), key -> new ArrayList<>()).add(comment);
        }

        // 루트 댓글 생성일 정렬 후 응답 변환
        rootComments.sort(Comparator.comparing(CommunityComment::getCreatedAt));
        return rootComments.stream()
                .map(comment -> createCommentResponse(comment, childCommentsByParentId, likedCommentById))
                .toList();
    }

    public void publishPostLikeAlarm(CommunityPost post, String actorHandle) {
        // 본인 게시글 좋아요면 알람 생략
        if (post.getHandle().equals(actorHandle)) {
            return;
        }

        // 게시글 좋아요 알람 발행
        communityAlarmPort.publishPostLike(
                post.getHandle(), actorHandle,
                CommunityPostIdPolicy.format(post.getPostId()), post.getTitle()
        );
    }

    public void publishCommentAlarms(CommunityPost post, CommunityComment comment,
                                     Optional<CommunityComment> parentComment, String actorHandle) {
        // 대댓글 대상 댓글 작성자에게 알람 발행
        boolean replyAlarmDelivered = publishReplyAlarmIfNeeded(post, comment, parentComment, actorHandle);

        // 본인 게시글 댓글이면 게시글 댓글 알람 생략
        if (post.getHandle().equals(actorHandle)) {
            return;
        }

        // 대댓글 알람 수신자가 게시글 작성자와 같으면 중복 알람 생략
        if (replyAlarmDelivered && parentComment.map(currentComment -> currentComment.getHandle().equals(post.getHandle())).orElse(false)) {
            return;
        }

        // 게시글 댓글 알람 발행
        communityAlarmPort.publishPostComment(
                post.getHandle(), actorHandle,
                CommunityPostIdPolicy.format(post.getPostId()), post.getTitle(), comment.getCommentId()
        );
    }

    public void publishCommentLikeAlarm(CommunityComment comment, String actorHandle) {
        // 본인 댓글 좋아요면 알람 생략
        if (comment.getHandle().equals(actorHandle)) {
            return;
        }

        // 댓글 좋아요 알람 발행
        communityAlarmPort.publishCommentLike(
                comment.getHandle(), actorHandle,
                CommunityPostIdPolicy.format(comment.getPostId()), comment.getContent(), comment.getCommentId()
        );
    }

    private CommunityCommentOutput createCommentResponse(CommunityComment comment,
                                                         Map<Long, List<CommunityComment>> childCommentsByParentId,
                                                         Map<Long, Boolean> likedCommentById) {
        // 대댓글 응답 재귀 생성
        List<CommunityCommentOutput> replies = childCommentsByParentId.getOrDefault(comment.getCommentId(), List.of()).stream()
                .sorted(Comparator.comparing(CommunityComment::getCreatedAt))
                .map(reply -> createCommentResponse(reply, childCommentsByParentId, likedCommentById))
                .toList();

        // 댓글 응답 변환
        return new CommunityCommentOutput(
                comment.getCommentId(), comment.getHandle(), comment.getContent(), comment.getCreatedAt(),
                comment.getLikeCount(), likedCommentById.getOrDefault(comment.getCommentId(), false), replies
        );
    }

    private boolean publishReplyAlarmIfNeeded(CommunityPost post, CommunityComment comment,
                                              Optional<CommunityComment> parentComment, String actorHandle) {
        // 대댓글 알람 대상 없으면 발행 생략
        if (parentComment.isEmpty() || parentComment.get().getHandle().equals(actorHandle)) {
            return false;
        }

        // 대댓글 알람 발행
        communityAlarmPort.publishCommentReply(
                parentComment.get().getHandle(), actorHandle,
                CommunityPostIdPolicy.format(post.getPostId()),
                parentComment.get().getContent(), parentComment.get().getCommentId(), comment.getCommentId()
        );
        return true;
    }
}
