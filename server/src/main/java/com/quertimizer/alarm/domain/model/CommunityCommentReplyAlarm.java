package com.quertimizer.alarm.domain.model;

import lombok.Value;

import java.util.Map;

@Value
public class CommunityCommentReplyAlarm implements AlarmSpec {

    String recipientHandle;
    String actorHandle;
    String postId;
    String replyContent;
    Long replyCommentId;

    public CommunityCommentReplyAlarm(String recipientHandle, String actorHandle,
                                      String postId, String replyContent, Long replyCommentId) {
        this.recipientHandle = recipientHandle;
        this.actorHandle = actorHandle;
        this.postId = postId;
        this.replyContent = replyContent;
        this.replyCommentId = replyCommentId;
    }

    @Override
    public String recipientHandle() {
        return recipientHandle;
    }

    @Override
    public String alarmType() {
        return AlarmType.REPLY_MY_COMMENT.getValue();
    }

    @Override
    public String title() {
        return AlarmType.REPLY_MY_COMMENT.getTitle();
    }

    @Override
    public String message() {
        return AlarmType.REPLY_MY_COMMENT.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + replyCommentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "comment", AlarmBinding.of(replyContent, "/community/" + postId, "#community-comment-" + replyCommentId)
        );
    }

}
