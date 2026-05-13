package com.quertimizer.alarm.domain.model;

import lombok.Getter;

@Getter
public enum AlarmType {

    FROM_ADMIN("FROM_ADMIN", "관리자 알람", "관리자 직접 알림", "관리자가 직접 전송한 알림", ""),
    LIKE_MY_POST(
            "LIKE_MY_POST",
            "게시글 좋아요",
            "{handle}님이 내 글 {title} 에 좋아요를 눌렀습니다.",
            "내 글에 좋아요 눌림.",
            "%s님이 게시글에 좋아요를 눌렀습니다."
    ),
    COMMENT_MY_POST(
            "COMMENT_MY_POST",
            "새 댓글",
            "{handle}님이 내 글 {title} 에 댓글을 남겼습니다.",
            "내 글에 댓글 남김.",
            "%s님이 게시글에 댓글을 남겼습니다."
    ),
    REPLY_MY_COMMENT(
            "REPLY_MY_COMMENT",
            "새 대댓글",
            "{handle}님이 내 댓글 {comment} 에 대댓글을 남겼습니다.",
            "내 댓글에 대댓글 남김.",
            "%s님이 댓글에 대댓글을 남겼습니다."
    ),
    LIKE_MY_COMMENT(
            "LIKE_MY_COMMENT",
            "댓글 좋아요",
            "{handle}님이 내 댓글 {comment} 에 좋아요를 눌렀습니다.",
            "내 댓글에 좋아요 눌림.",
            "%s님이 댓글에 좋아요를 눌렀습니다."
    );

    private final String value;
    private final String title;
    private final String defaultSentence;
    private final String defaultDescription;
    private final String defaultMessageTemplate;

    AlarmType(String value, String title, String defaultSentence, String defaultDescription, String defaultMessageTemplate) {
        this.value = value;
        this.title = title;
        this.defaultSentence = defaultSentence;
        this.defaultDescription = defaultDescription;
        this.defaultMessageTemplate = defaultMessageTemplate;
    }

    public String formatDefaultMessage(String actorHandle) {
        return defaultMessageTemplate.formatted(actorHandle);
    }

}
