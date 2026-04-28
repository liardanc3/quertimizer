package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.CommunityPostDetailInput;
import com.quertimizer.community.application.output.CommunityPostDetailOutput;
import com.quertimizer.community.application.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetCommunityPostDetail {

    private final CommunityService communityService;

    /**
     * 게시글 상세를 현재 사용자 반응 정보와 함께 조회한다.
     *
     * @param input 조회할 게시글과 현재 사용자 입력
     */
    public Optional<CommunityPostDetailOutput> execute(CommunityPostDetailInput input) {
        return communityService.getPostDetail(input.getPostId(), input.getCurrentHandle());
    }
}
