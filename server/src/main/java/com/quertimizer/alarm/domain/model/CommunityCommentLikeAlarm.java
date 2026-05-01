package com.quertimizer.alarm.domain.model;

import lombok.Value;

import java.util.Map;

@Value
public class CommunityCommentLikeAlarm implements AlarmSpec {

    String recipientHandle;
    String actorHandle;
    String postId;
    String commentContent;
    Long commentId;

    public CommunityCommentLikeAlarm(String recipientHandle, String actorHandle,
                                     String postId, String commentContent, Long commentId) {
        this.recipientHandle = recipientHandle;
        this.actorHandle = actorHandle;
        this.postId = postId;
        this.commentContent = commentContent;
        this.commentId = commentId;
    }

    @Override
    public String recipientHandle() {
        return recipientHandle;
    }

    @Override
    public String alarmType() {
        return AlarmType.LIKE_MY_COMMENT.getValue();
    }

    @Override
    public String title() {
        return AlarmType.LIKE_MY_COMMENT.getTitle();
    }

    @Override
    public String message() {
        return AlarmType.LIKE_MY_COMMENT.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + commentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "comment", AlarmBinding.of(commentContent, "/community/" + postId, "#community-comment-" + commentId)
        );
    }

}
