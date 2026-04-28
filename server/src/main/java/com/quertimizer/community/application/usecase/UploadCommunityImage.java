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

    /**
     * 커뮤니티 글쓰기 이미지를 업로드한다.
     *
     * @param file 업로드할 이미지 파일
     */
    public CommunityImageOutput execute(MultipartFile file) {
        return communityImageService.uploadImage(file);
    }
}
