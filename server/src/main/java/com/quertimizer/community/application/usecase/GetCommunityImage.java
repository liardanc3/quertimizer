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

    public Optional<CommunityImageOutput> execute(String imageId) {
        // 커뮤니티 이미지를 조회
        return communityImageService.getImage(imageId);
    }
}
