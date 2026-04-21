package com.quertimizer.alarm;

import java.util.Map;

public record CommunityPostLikeAlarm(String recipientUserId, String actorUserId, String postId, String postTitle) implements AlarmSpec {

    @Override
    public String alarmType() {
        return "LIKE_MY_POST";
    }

    @Override
    public String title() {
        return "게시글 좋아요";
    }

    @Override
    public String message() {
        return actorUserId + "님이 게시글에 좋아요를 눌렀다.";
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-post-detail");
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorUserId, "/profile/" + actorUserId),
                "title", AlarmBinding.of(postTitle, "/community/" + postId, "#community-post-detail")
        );
    }

}
