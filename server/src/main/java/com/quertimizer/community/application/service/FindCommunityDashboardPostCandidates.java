package com.quertimizer.community.application.service;

import com.quertimizer.community.application.output.CommunityDashboardPostCandidateOutput;
import com.quertimizer.community.application.port.in.FindCommunityDashboardPostCandidatesUseCase;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FindCommunityDashboardPostCandidates implements FindCommunityDashboardPostCandidatesUseCase {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostTagRepositoryPort communityPostTagRepository;

    /**
     * 대시보드 커뮤니티 후보 게시글을 조회한다.
     *
     * <ol>
     *   <li>게시글과 태그 조회
     *   <li>대시보드 전용 응답 변환
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommunityDashboardPostCandidateOutput> execute() {
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        return posts.stream()
                .map(post -> toCandidate(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 번호가 없으면 빈 태그 map 반환
        if (postIds.isEmpty()) {
            return Map.of();
        }

        // 게시글 번호별 태그 목록 수집
        Map<Long, List<String>> tagsByPostId = new LinkedHashMap<>();
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>()).add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private CommunityDashboardPostCandidateOutput toCandidate(CommunityPost post, List<String> tags) {
        // 커뮤니티 게시글을 대시보드 후보 응답으로 변환
        return new CommunityDashboardPostCandidateOutput(
                CommunityPostIdPolicy.format(post.getPostId()), post.getTitle(), post.getHandle(),
                post.getContentJson(), post.getPlainTextSummary(), tags, resolveCategory(post), post.getCreatedAt(),
                post.getViewCount(), post.getLikeCount(), post.getCommentCount()
        );
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
