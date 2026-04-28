package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityImageOutput;
import com.quertimizer.community.application.service.CommunityImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetCommunityImage {

    private final CommunityImageService communityImageService;

    /**
     * 커뮤니티 이미지를 조회한다.
     *
     * @param imageId 조회할 이미지 ID
     */
    public Optional<CommunityImageOutput> execute(String imageId) {
        return communityImageService.getImage(imageId);
    }
}
