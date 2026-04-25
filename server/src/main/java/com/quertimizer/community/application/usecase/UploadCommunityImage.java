package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.output.CommunityImageOutput;
import com.quertimizer.community.application.service.CommunityImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class UploadCommunityImage {

    private final CommunityImageService communityImageService;

    public CommunityImageOutput execute(MultipartFile file) {
        // 커뮤니티 글쓰기 이미지를 업로드
        return communityImageService.uploadImage(file);
    }
}
