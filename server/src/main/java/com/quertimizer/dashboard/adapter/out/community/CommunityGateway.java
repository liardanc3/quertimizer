package com.quertimizer.dashboard.adapter.out.community;

import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.dashboard.application.port.out.DashboardCommunityPort;
import com.quertimizer.dashboard.domain.model.DashboardCommunityPostCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("dashboardCommunityGateway")
@RequiredArgsConstructor
public class CommunityGateway implements DashboardCommunityPort {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostTagRepositoryPort communityPostTagRepository;

    @Override
    public List<DashboardCommunityPostCandidate> findCommunityPostCandidates() {
        // 커뮤니티 게시글과 태그 조회
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        // 대시보드 후보 모델 변환
        return posts.stream()
                .map(post -> toCandidate(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 태그 결과 저장소 준비
        Map<Long, List<String>> tagsByPostId = new LinkedHashMap<>();

        // 조회 대상 게시글 번호 없으면 빈 결과 반환
        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        // 게시글 번호별 태그 목록 수집
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>()).add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private DashboardCommunityPostCandidate toCandidate(CommunityPost post, List<String> tags) {
        // 커뮤니티 게시글을 대시보드 후보 모델로 변환
        return new DashboardCommunityPostCandidate(
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
