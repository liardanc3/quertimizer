package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityPostDetailOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetCommunityPostDetail {

    private final CommunityService communityService;

    public Optional<CommunityPostDetailOutput> execute(Long postId, String currentHandle) {
        // 게시글 상세를 조회
        return communityService.getPostDetail(postId, currentHandle);
    }
}
