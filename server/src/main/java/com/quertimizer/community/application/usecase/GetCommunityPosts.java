package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityPostSearchInput;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostSearchPort;
import com.quertimizer.community.application.service.CommunityService;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.model.CommunityPostConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetCommunityPosts {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;

    /**
     * 커뮤니티 게시글 목록을 검색 조건에 맞게 조회한다.
     *
     * <ol>
     *   <li>게시글과 태그 목록 조회
     *   <li>검색 포트로 검색, 필터, 정렬, 페이징 수행
     * </ol>
     *
     * @param input 게시글 검색, 필터, 정렬 입력
     */
    @Transactional(readOnly = true)
    public CommunityPostPageOutput execute(CommunityPostSearchInput input) {
        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = communityService.createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());
        return communityPostSearchPort.searchPosts(
                input.getPage(), CommunityPostConstant.PAGE_SIZE,
                input.getSearch(), input.getTag(), input.getCategory(), input.getSortKey(),
                posts, tagsByPostId
        );
    }
}
