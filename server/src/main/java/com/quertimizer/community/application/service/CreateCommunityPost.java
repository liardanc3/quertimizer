package com.quertimizer.community.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.community.application.port.in.CreateCommunityPostUseCase;
import com.quertimizer.community.application.input.CreateCommunityPostInput;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostSearchPort;
import com.quertimizer.community.application.port.out.CommunityUserPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.policy.CommunityNoticePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateCommunityPost implements CreateCommunityPostUseCase {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;
    private final CommunityContentValidationService communityContentValidationService;
    private final CommunityNoticePolicy communityNoticePolicy;
    private final CommunityUserPort communityUserPort;

    /**
     * 커뮤니티 게시글을 생성한다.
     *
     * <ol>
     *   <li>게시글 입력 정규화와 정책 검증
     *   <li>게시글 저장
     *   <li>태그 저장과 검색 인덱스 동기화
     * </ol>
     *
     * @param input 게시글 작성자와 저장할 게시글 입력
     */
    @Transactional
    @Override
    @Log("커뮤니티 게시글 생성")
    public Long execute(CreateCommunityPostInput input) {
        String normalizedTitle = input.getPost().getTitle().trim();
        String normalizedContentJson = communityService.normalizeContentJson(input.getPost().getContentJson());
        communityContentValidationService.validate(normalizedContentJson);
        String normalizedPlainTextSummary = communityService.normalizePlainTextSummary(input.getPost().getPlainTextSummary());
        String normalizedImageIds = communityService.normalizeImageIds(input.getPost().getImageIds());
        List<String> normalizedTags = communityService.normalizeTags(input.getPost().getTags());
        String normalizedCategory = communityService.normalizePostCategory(input.getPost().getCategory());
        communityNoticePolicy.validateNoticeWritable(communityUserPort.findRole(input.getHandle()), "", normalizedCategory);

        CommunityPost post = communityPostRepository.save(CommunityPost.create(
                createNextPostId(), input.getHandle(), normalizedTitle, normalizedContentJson,
                normalizedPlainTextSummary, normalizedImageIds, normalizedCategory
        ));
        communityService.replaceTags(post.getPostId(), normalizedTags);
        communityPostSearchPort.syncPost(post, normalizedTags);
        return post.getPostId();
    }

    private Long createNextPostId() {
        // 다음 게시글 번호 생성
        return communityPostRepository.findTopPostId()
                .map(postId -> postId + 1)
                .orElse(1L);
    }

}
