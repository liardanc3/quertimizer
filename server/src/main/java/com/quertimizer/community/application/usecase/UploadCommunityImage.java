package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityImageOutput;
import com.quertimizer.community.application.service.CommunityImageService;
import com.quertimizer.community.domain.model.CommunityImageFileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class UploadCommunityImage {

    private final CommunityImageService communityImageService;

    /**
     * 커뮤니티 글쓰기 이미지를 업로드한다.
     *
     * <ol>
     *   <li>이미지 파일 검증
     *   <li>이미지 저장 번호와 경로 생성
     *   <li>이미지 파일 저장 후 응답 생성
     * </ol>
     *
     * @param file 업로드할 이미지 파일
     */
    public CommunityImageOutput execute(MultipartFile file) {
        CommunityImageFileInfo imageFileInfo = communityImageService.validateImageFile(file);
        String imageId = communityImageService.createImageId(imageFileInfo);
        Path targetPath = communityImageService.resolveImagePath(imageId);
        communityImageService.copyImage(file, targetPath);
        return communityImageService.createUploadOutput(imageId, imageFileInfo);
    }
}
