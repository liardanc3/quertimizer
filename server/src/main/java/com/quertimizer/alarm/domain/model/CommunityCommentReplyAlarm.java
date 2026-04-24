package com.quertimizer.alarm.domain.model;

import java.util.Map;

public record CommunityCommentReplyAlarm(String recipientHandle,
                                         String actorHandle,
                                         String postId,
                                         String replyContent,
                                         Long replyCommentId) implements AlarmSpec {

    @Override
    public String alarmType() {
        // alarm 유형 처리
        return AlarmType.REPLY_MY_COMMENT.getValue();
    }

    @Override
    public String title() {
        // title 처리
        return AlarmType.REPLY_MY_COMMENT.getTitle();
    }

    @Override
    public String message() {
        // message 처리
        return AlarmType.REPLY_MY_COMMENT.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        // target 처리
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + replyCommentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        // bindings 처리
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "comment", AlarmBinding.of(replyContent, "/community/" + postId, "#community-comment-" + replyCommentId)
        );
    }

}
