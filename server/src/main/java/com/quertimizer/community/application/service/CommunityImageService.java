package com.quertimizer.community.application.service;

import com.quertimizer.community.application.output.CommunityImageOutput;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityImageService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.community.image-storage-path:build/community-images}")
    private String imageStoragePath;

    public CommunityImageOutput uploadImage(MultipartFile file) {
        // 이미지 파일을 서버 저장소에 저장
        validateImageFile(file);
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String imageId = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = resolveStorageRoot().resolve(imageId);

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new CommunityImageOutput(imageId, "/community/images/" + imageId, null, file.getContentType());
    }

    public Optional<CommunityImageOutput> getImage(String imageId) {
        // 저장된 이미지 파일을 조회
        if (!isSafeImageId(imageId)) {
            return Optional.empty();
        }

        Path imagePath = resolveStorageRoot().resolve(imageId).normalize();

        if (!imagePath.startsWith(resolveStorageRoot()) || !Files.exists(imagePath)) {
            return Optional.empty();
        }

        try {
            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Optional.ofNullable(Files.probeContentType(imagePath)).orElse("application/octet-stream");
            return Optional.of(new CommunityImageOutput(imageId, "/community/images/" + imageId, resource, contentType));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private void validateImageFile(MultipartFile file) {
        // 업로드 이미지 파일을 검증
        if (file == null || file.isEmpty()) {
            throw new BusinessException("이미지 파일을 첨부해 주세요.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("이미지는 최대 10MB까지 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        // 파일 확장자를 결정
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }

        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }

        if ("image/gif".equalsIgnoreCase(contentType)) {
            return "gif";
        }

        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }

        return "jpg";
    }

    private boolean isSafeImageId(String imageId) {
        // 이미지 번호 경로 안전성 확인
        return StringUtils.hasText(imageId) && imageId.matches("[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)");
    }

    private Path resolveStorageRoot() {
        // 이미지 저장소 경로 결정
        return Path.of(imageStoragePath).toAbsolutePath().normalize();
    }
}
