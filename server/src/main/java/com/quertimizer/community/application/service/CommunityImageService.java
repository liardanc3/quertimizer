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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityImageService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.community.image-storage-path:build/community-images}")
    private String imageStoragePath;

    public CommunityImageOutput uploadImage(MultipartFile file) {
        // 이미지 파일을 서버 저장소에 저장
        ImageFileInfo imageFileInfo = validateImageFile(file);
        String imageId = UUID.randomUUID().toString().replace("-", "") + "." + imageFileInfo.extension();
        Path targetPath = resolveStorageRoot().resolve(imageId);

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new CommunityImageOutput(imageId, "/community/images/" + imageId, null, imageFileInfo.contentType());
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

    private ImageFileInfo validateImageFile(MultipartFile file) {
        // 업로드 이미지 파일을 검증
        if (file == null || file.isEmpty()) {
            throw new BusinessException("이미지 파일을 첨부해 주세요.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("이미지는 최대 10MB까지 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        ImageFileInfo imageFileInfo = resolveImageFileInfo(file);
        if (!ALLOWED_IMAGE_TYPES.contains(imageFileInfo.extension())) {
            throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
        }

        validateImageDimensions(file, imageFileInfo);
        return imageFileInfo;
    }

    private ImageFileInfo resolveImageFileInfo(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(32);
            if (isJpeg(header)) {
                return new ImageFileInfo("jpg", "image/jpeg");
            }
            if (isPng(header)) {
                return new ImageFileInfo("png", "image/png");
            }
            if (isGif(header)) {
                return new ImageFileInfo("gif", "image/gif");
            }
            if (isWebp(header)) {
                return new ImageFileInfo("webp", "image/webp");
            }
            if (looksLikeSvg(header)) {
                throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
    }

    private void validateImageDimensions(MultipartFile file, ImageFileInfo imageFileInfo) {
        if ("webp".equals(imageFileInfo.extension())) {
            return;
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
            }

            long pixels = (long) image.getWidth() * image.getHeight();
            if (pixels <= 0 || pixels > MAX_IMAGE_PIXELS) {
                throw new BusinessException("이미지 크기가 너무 큽니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        return header.length >= png.length && Arrays.equals(Arrays.copyOf(header, png.length), png);
    }

    private boolean isGif(byte[] header) {
        return header.length >= 6
                && (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a"));
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12 && startsWithAscii(header, "RIFF") && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII));
    }

    private boolean looksLikeSvg(byte[] header) {
        String prefix = new String(header, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        return prefix.startsWith("<svg") || prefix.startsWith("<?xml");
    }

    private boolean startsWithAscii(byte[] header, String expected) {
        if (header.length < expected.length()) {
            return false;
        }

        for (int index = 0; index < expected.length(); index++) {
            if (header[index] != (byte) expected.charAt(index)) {
                return false;
            }
        }

        return true;
    }

    private boolean isSafeImageId(String imageId) {
        // 이미지 번호 경로 안전성 확인
        return StringUtils.hasText(imageId) && imageId.matches("[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)");
    }

    private Path resolveStorageRoot() {
        // 이미지 저장소 경로 결정
        return Path.of(imageStoragePath).toAbsolutePath().normalize();
    }

    private record ImageFileInfo(String extension, String contentType) {
    }
}
