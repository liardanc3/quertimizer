package com.quertimizer.alarm.domain.model;

import lombok.Value;

import java.util.Map;

@Value
public class CommunityPostLikeAlarm implements AlarmSpec {

    String recipientHandle;
    String actorHandle;
    String postId;
    String postTitle;

    public CommunityPostLikeAlarm(String recipientHandle, String actorHandle, String postId, String postTitle) {
        this.recipientHandle = recipientHandle;
        this.actorHandle = actorHandle;
        this.postId = postId;
        this.postTitle = postTitle;
    }

    @Override
    public String recipientHandle() {
        return recipientHandle;
    }

    @Override
    public String alarmType() {
        return AlarmType.LIKE_MY_POST.getValue();
    }

    @Override
    public String title() {
        return AlarmType.LIKE_MY_POST.getTitle();
    }

    @Override
    public String message() {
        return AlarmType.LIKE_MY_POST.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-post-detail");
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "title", AlarmBinding.of(postTitle, "/community/" + postId, "#community-post-detail")
        );
    }

}
