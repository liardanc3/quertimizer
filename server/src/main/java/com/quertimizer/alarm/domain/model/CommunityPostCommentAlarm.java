package com.quertimizer.alarm.domain.model;

import java.util.Map;

public record CommunityPostCommentAlarm(String recipientHandle,
                                        String actorHandle,
                                        String postId,
                                        String commentContent,
                                        Long commentId) implements AlarmSpec {

    @Override
    public String alarmType() {
        // alarm 유형 처리
        return AlarmType.COMMENT_MY_POST.getValue();
    }

    @Override
    public String title() {
        // title 처리
        return AlarmType.COMMENT_MY_POST.getTitle();
    }

    @Override
    public String message() {
        // message 처리
        return AlarmType.COMMENT_MY_POST.formatDefaultMessage(actorHandle);
    }

    @Override
    public AlarmTarget target() {
        // target 처리
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + commentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        // bindings 처리
        return Map.of(
                "handle", AlarmBinding.of(actorHandle, "/profile/" + actorHandle),
                "comment", AlarmBinding.of(commentContent, "/community/" + postId, "#community-comment-" + commentId)
        );
    }

}
