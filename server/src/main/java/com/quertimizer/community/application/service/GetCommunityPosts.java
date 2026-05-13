package com.quertimizer.community.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.community.application.port.in.GetCommunityPostsUseCase;
import com.quertimizer.community.application.input.CommunityPostSearchInput;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.output.CommunityPostSummaryOutput;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostSearchPort;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.model.CommunityPostConstant;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetCommunityPosts implements GetCommunityPostsUseCase {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostTagRepositoryPort communityPostTagRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글 목록을 검색 조건에 맞게 조회한다.
     *
     * <ol>
     *   <li>태그 조건 있으면 태그 저장소 기준 exact 조회
     *   <li>태그 조건 없으면 검색 포트로 검색, 필터, 정렬, 페이징 수행
     * </ol>
     *
     * @param input 게시글 검색, 필터, 정렬 입력
     */
    @Transactional(readOnly = true)
    @Override
    @Log("커뮤니티 게시글 목록 조회")
    public CommunityPostPageOutput execute(CommunityPostSearchInput input) {
        if (StringUtils.hasText(input.getTag())) {
            return createExactTaggedPostPage(input);
        }

        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = communityService.createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());
        return communityPostSearchPort.searchPosts(
                input.getPage(), CommunityPostConstant.PAGE_SIZE,
                input.getSearch(), input.getTag(), input.getCategory(), input.getSortKey(),
                posts, tagsByPostId
        );
    }

    private CommunityPostPageOutput createExactTaggedPostPage(CommunityPostSearchInput input) {
        // exact 태그가 연결된 게시글 번호 조회
        List<Long> postIds = communityPostTagRepository.findAllByTagOrderByPostIdAscTagOrderAsc(input.getTag().trim()).stream()
                .map(CommunityPostTag::getPostId)
                .distinct()
                .toList();

        // exact 태그 게시글 없으면 빈 페이지 반환
        if (postIds.isEmpty()) {
            return new CommunityPostPageOutput(1, CommunityPostConstant.PAGE_SIZE, 0, 1, List.of());
        }

        // 태그와 구분 조건 구성
        Map<Long, List<String>> tagsByPostId = communityService.createTagsByPostId(postIds);
        String category = !StringUtils.hasText(input.getCategory()) || "all".equalsIgnoreCase(input.getCategory().trim())
                ? "all"
                : communityService.normalizePostCategory(input.getCategory());

        // 게시글 정렬 기준 구성
        Comparator<CommunityPost> postComparator = Comparator.comparing(CommunityPost::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(CommunityPost::getPostId);
        if ("oldest".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparing(CommunityPost::getCreatedAt)
                    .thenComparing(CommunityPost::getPostId);
        } else if ("views".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getViewCount).reversed()
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        } else if ("viewsAsc".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getViewCount)
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        } else if ("likes".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getLikeCount).reversed()
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        } else if ("likesAsc".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getLikeCount)
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        } else if ("comments".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getCommentCount).reversed()
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        } else if ("commentsAsc".equalsIgnoreCase(input.getSortKey())) {
            postComparator = Comparator.comparingInt(CommunityPost::getCommentCount)
                    .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder());
        }

        // exact 태그 게시글 기준 구분 필터와 정렬 적용
        List<CommunityPost> posts = communityPostRepository.findAllByPostIdIn(postIds).stream()
                .filter(post -> "all".equals(category) || category.equals(communityService.resolveCategory(post)))
                .sorted(postComparator)
                .toList();

        // 현재 페이지 범위 계산
        int totalCount = posts.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) CommunityPostConstant.PAGE_SIZE));
        int currentPage = Math.min(Math.max(input.getPage(), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * CommunityPostConstant.PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + CommunityPostConstant.PAGE_SIZE, totalCount);

        // 페이지 게시글 요약 응답 생성
        return new CommunityPostPageOutput(
                currentPage, CommunityPostConstant.PAGE_SIZE,
                totalCount, totalPages,
                posts.subList(fromIndex, toIndex).stream()
                        .map(post -> {
                            String summary = StringUtils.hasText(post.getPlainTextSummary()) ? post.getPlainTextSummary().trim() : "";
                            String excerpt = summary.length() > 120 ? summary.substring(0, 120).trim() + "..." : summary;
                            return new CommunityPostSummaryOutput(
                                    CommunityPostIdPolicy.format(post.getPostId()), post.getTitle(), post.getHandle(), excerpt,
                                    tagsByPostId.getOrDefault(post.getPostId(), List.of()), communityService.resolveCategory(post),
                                    post.getCreatedAt(), post.getUpdatedAt(),
                                    post.getViewCount(), post.getLikeCount(), post.getCommentCount()
                            );
                        })
                        .toList()
        );
    }
}
