package com.quertimizer.community.application.service;

import com.quertimizer.community.application.port.in.GetCommunityImageUseCase;
import com.quertimizer.community.application.output.CommunityImageOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetCommunityImage implements GetCommunityImageUseCase {

    private final CommunityImageService communityImageService;

    /**
     * 커뮤니티 이미지를 조회한다.
     *
     * <ol>
     *   <li>이미지 번호 경로 안전성 검사
     *   <li>이미지 저장 경로와 존재 여부 확인
     *   <li>저장 이미지 응답 생성
     * </ol>
     *
     * @param imageId 조회할 이미지 ID
     */
    @Override
    public Optional<CommunityImageOutput> execute(String imageId) {
        if (!communityImageService.isSafeImageId(imageId)) {
            return Optional.empty();
        }

        Path imagePath = communityImageService.resolveImagePath(imageId);
        if (!communityImageService.existsInStorage(imagePath)) {
            return Optional.empty();
        }

        return communityImageService.createStoredImageOutput(imageId, imagePath);
    }
}
