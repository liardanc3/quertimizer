package com.quertimizer.alarm.domain.model;

import lombok.Value;

import java.util.Map;

@Value
public class CommunityPostCommentAlarm implements AlarmSpec {

    String recipientHandle;
    String actorHandle;
    String postId;
    String postTitle;
    Long commentId;

    public CommunityPostCommentAlarm(String recipientHandle, String actorHandle,
                                     String postId, String postTitle, Long commentId) {
        this.recipientHandle = recipientHandle;
        this.actorHandle = actorHandle;
        this.postId = postId;
        this.postTitle = postTitle;
        this.commentId = commentId;
    }

    @Override
    public String recipientHandle() {
        return recipientHandle;
    }

    @Override
    public String alarmType() {
        return AlarmType.COMMENT_MY_POST.getValue();
    }

    @Override
    public String title() {
        return AlarmType.COMMENT_MY_POST.getTitle();
    }

    @Override
    public String message() {
        return AlarmType.COMMENT_MY_POST.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + commentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "title", AlarmBinding.of(postTitle, "/community/" + postId, "#community-comment-" + commentId)
        );
    }

}
