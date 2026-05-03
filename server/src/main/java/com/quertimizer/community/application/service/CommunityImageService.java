package com.quertimizer.community.application.service;

import com.quertimizer.community.application.input.CommunityImageUploadInput;
import com.quertimizer.community.application.output.CommunityImageOutput;
import com.quertimizer.community.domain.model.CommunityImageConstant;
import com.quertimizer.community.domain.model.CommunityImageFileInfo;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityImageService {

    @Value("${app.community.image-storage-path:build/community-images}")
    private String imageStoragePath;

    public CommunityImageFileInfo validateImageFile(CommunityImageUploadInput input) {
        // 업로드 이미지 파일 존재 여부 검사
        if (input == null || input.isEmpty()) {
            throw new BusinessException("이미지 파일을 첨부해 주세요.", HttpStatus.BAD_REQUEST);
        }

        // 업로드 이미지 파일 크기 제한 검사
        if (input.getSize() > CommunityImageConstant.MAX_SIZE) {
            throw new BusinessException("이미지는 최대 10MB까지 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        // 업로드 이미지 파일 형식과 픽셀 수 검사
        CommunityImageFileInfo imageFileInfo = resolveImageFileInfo(input.getContent());
        if (!CommunityImageConstant.ALLOWED_TYPES.contains(imageFileInfo.getExtension())) {
            throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
        }

        validateImageDimensions(input.getContent(), imageFileInfo);
        return imageFileInfo;
    }

    public String createImageId(CommunityImageFileInfo imageFileInfo) {
        // 이미지 저장 번호 생성
        return UUID.randomUUID().toString().replace("-", "") + "." + imageFileInfo.getExtension();
    }

    public Path resolveImagePath(String imageId) {
        // 이미지 저장 경로 결정
        return resolveStorageRoot().resolve(imageId).normalize();
    }

    public void copyImage(CommunityImageUploadInput input, Path targetPath) {
        // 이미지 파일 저장소 복사
        try (InputStream inputStream = new ByteArrayInputStream(input.getContent())) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public CommunityImageOutput createUploadOutput(String imageId, CommunityImageFileInfo imageFileInfo) {
        // 업로드 이미지 응답 생성
        return new CommunityImageOutput(imageId, "/community/images/" + imageId, null, imageFileInfo.getContentType());
    }

    public boolean existsInStorage(Path imagePath) {
        // 저장소 내부에 존재하는 이미지 경로 여부 확인
        if (!imagePath.startsWith(resolveStorageRoot()) || !Files.exists(imagePath)) {
            return false;
        }

        return true;
    }

    public Optional<CommunityImageOutput> createStoredImageOutput(String imageId, Path imagePath) {
        // 저장된 이미지 응답 생성
        try {
            String contentType = Optional.ofNullable(Files.probeContentType(imagePath)).orElse("application/octet-stream");
            return Optional.of(new CommunityImageOutput(imageId, "/community/images/" + imageId, imagePath, contentType));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private CommunityImageFileInfo resolveImageFileInfo(byte[] content) {
        // 파일 헤더 시그니처 기준 이미지 형식 판별
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            byte[] header = inputStream.readNBytes(32);
            if (isJpeg(header)) {
                return new CommunityImageFileInfo("jpg", "image/jpeg");
            }
            if (isPng(header)) {
                return new CommunityImageFileInfo("png", "image/png");
            }
            if (isGif(header)) {
                return new CommunityImageFileInfo("gif", "image/gif");
            }
            if (isWebp(header)) {
                return new CommunityImageFileInfo("webp", "image/webp");
            }
            if (looksLikeSvg(header)) {
                throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
    }

    private void validateImageDimensions(byte[] content, CommunityImageFileInfo imageFileInfo) {
        // 디코딩 가능한 이미지 픽셀 수 제한
        if ("webp".equals(imageFileInfo.getExtension())) {
            return;
        }

        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException("지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
            }

            long pixels = (long) image.getWidth() * image.getHeight();
            if (pixels <= 0 || pixels > CommunityImageConstant.MAX_PIXELS) {
                throw new BusinessException("이미지 크기가 너무 큽니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isJpeg(byte[] header) {
        // JPEG 파일 시그니처 확인
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        // PNG 파일 시그니처 확인
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        return header.length >= png.length && Arrays.equals(Arrays.copyOf(header, png.length), png);
    }

    private boolean isGif(byte[] header) {
        // GIF 파일 시그니처 확인
        return header.length >= 6
                && (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a"));
    }

    private boolean isWebp(byte[] header) {
        // WEBP 파일 시그니처 확인
        return header.length >= 12 && startsWithAscii(header, "RIFF") && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII));
    }

    private boolean looksLikeSvg(byte[] header) {
        // SVG/XML 계열 텍스트 이미지 차단용 헤더 확인
        String prefix = new String(header, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        return prefix.startsWith("<svg") || prefix.startsWith("<?xml");
    }

    private boolean startsWithAscii(byte[] header, String expected) {
        // ASCII 문자열 prefix 일치 여부 확인
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

    public boolean isSafeImageId(String imageId) {
        // 이미지 번호 경로 안전성 확인
        return StringUtils.hasText(imageId) && imageId.matches("[a-fA-F0-9]{32}\\.(jpg|jpeg|png|gif|webp)");
    }

    private Path resolveStorageRoot() {
        // 이미지 저장소 경로 결정
        return Path.of(imageStoragePath).toAbsolutePath().normalize();
    }
}
