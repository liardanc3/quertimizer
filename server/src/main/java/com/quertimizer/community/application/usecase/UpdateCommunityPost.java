package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.UpdateCommunityPostInput;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostSearchPort;
import com.quertimizer.community.application.service.CommunityService;
import com.quertimizer.community.domain.policy.CommunityContentPolicy;
import com.quertimizer.community.domain.policy.CommunityNoticePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateCommunityPost {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;
    private final CommunityContentPolicy communityContentPolicy;
    private final CommunityNoticePolicy communityNoticePolicy;

    /**
     * 커뮤니티 게시글을 수정한다.
     *
     * <ol>
     *   <li>수정 가능한 게시글 조회
     *   <li>게시글 입력 정규화와 정책 검증
     *   <li>게시글, 태그, 검색 인덱스 갱신
     * </ol>
     *
     * @param input 수정할 게시글, 요청자, 저장할 게시글 입력
     */
    @Transactional
    public Optional<Long> execute(UpdateCommunityPostInput input) {
        return communityPostRepository.findById(input.getPostId())
                .filter(post -> post.getHandle().equals(input.getHandle()))
                .map(post -> {
                    String normalizedTitle = input.getPost().getTitle().trim();
                    String normalizedContentJson = communityService.normalizeContentJson(input.getPost().getContentJson());
                    communityContentPolicy.validate(normalizedContentJson);
                    String normalizedPlainTextSummary = communityService.normalizePlainTextSummary(input.getPost().getPlainTextSummary());
                    String normalizedImageIds = communityService.normalizeImageIds(input.getPost().getImageIds());
                    List<String> normalizedTags = communityService.normalizeTags(input.getPost().getTags());
                    String normalizedCategory = communityService.normalizePostCategory(input.getPost().getCategory());
                    communityNoticePolicy.validateNoticeWritable(
                            input.getHandle(), communityService.normalizePostCategory(post.getCategory()), normalizedCategory
                    );

                    post.changeContent(normalizedTitle, normalizedContentJson, normalizedPlainTextSummary, normalizedImageIds, normalizedCategory);
                    communityService.replaceTags(input.getPostId(), normalizedTags);
                    communityPostSearchPort.syncPost(post, normalizedTags);
                    return input.getPostId();
                });
    }
}
