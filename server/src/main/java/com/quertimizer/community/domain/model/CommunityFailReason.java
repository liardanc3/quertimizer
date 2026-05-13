package com.quertimizer.community.domain.model;

import lombok.Getter;

@Getter
public enum CommunityFailReason {

    IMAGE_REQUIRED("이미지 파일을 첨부해 주세요."),
    IMAGE_SIZE_EXCEEDED("이미지는 최대 10MB까지 업로드할 수 있습니다."),
    IMAGE_PIXEL_SIZE_EXCEEDED("이미지 크기가 너무 큽니다."),
    IMAGE_TYPE_UNSUPPORTED("지원하지 않는 이미지 형식입니다."),
    IMAGE_UPLOAD_FAILED("이미지 업로드에 실패했습니다."),
    CONTENT_INVALID("본문 형식이 올바르지 않습니다."),
    CONTENT_SIZE_EXCEEDED("본문은 최대 500000 Byte까지 입력할 수 있습니다."),
    NOTICE_WRITE_DENIED("공지 게시글은 관리자만 작성하거나 수정할 수 있습니다.");

    private final String message;

    CommunityFailReason(String message) {
        this.message = message;
    }

}
