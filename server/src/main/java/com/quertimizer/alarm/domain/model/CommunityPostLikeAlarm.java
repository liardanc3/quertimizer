package com.quertimizer.alarm.domain.model;

import java.util.Map;

public record CommunityPostLikeAlarm(String recipientHandle, String actorHandle, String postId, String postTitle) implements AlarmSpec {

    @Override
    public String alarmType() {
        // alarm 유형 처리
        return AlarmType.LIKE_MY_POST.getValue();
    }

    @Override
    public String title() {
        // title 처리
        return AlarmType.LIKE_MY_POST.getTitle();
    }

    @Override
    public String message() {
        // message 처리
        return AlarmType.LIKE_MY_POST.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        // target 처리
        return AlarmTarget.of("/community/" + postId, "#community-post-detail");
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        // bindings 처리
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "title", AlarmBinding.of(postTitle, "/community/" + postId, "#community-post-detail")
        );
    }

}
