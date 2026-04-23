package com.quertimizer.community.domain.model;

public final class CommunityValidationMessage {

    public static final String POST_TITLE_REQUIRED = "게시글 제목을 입력해.";
    public static final String POST_TITLE_LENGTH_EXCEEDED = "게시글 제목은 최대 200자까지 입력할 수 있다.";
    public static final String POST_CONTENT_LENGTH_EXCEEDED = "게시글 본문은 최대 100000자까지 입력할 수 있다.";
    public static final String TAG_LIMIT_EXCEEDED = "태그는 최대 10개까지 추가할 수 있다.";
    public static final String TAG_REQUIRED = "태그는 비워둘 수 없다.";
    public static final String TAG_LENGTH_EXCEEDED = "태그는 최대 100자까지 입력할 수 있다.";
    public static final String COMMENT_REQUIRED = "댓글 내용을 입력해.";
    public static final String COMMENT_LENGTH_EXCEEDED = "댓글은 최대 5000자까지 입력할 수 있다.";

    private CommunityValidationMessage() {
    }

}
