package com.quertimizer.alarm;

import java.util.Map;

public record CommunityPostCommentAlarm(String recipientUserId,
                                        String actorUserId,
                                        String postId,
                                        String commentContent,
                                        Long commentId) implements AlarmSpec {

    @Override
    public String alarmType() {
        return "COMMENT_MY_POST";
    }

    @Override
    public String title() {
        return "새 댓글";
    }

    @Override
    public String message() {
        return actorUserId + "님이 게시글에 댓글을 남겼다.";
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + commentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorUserId, "/profile/" + actorUserId),
                "comment", AlarmBinding.of(commentContent, "/community/" + postId, "#community-comment-" + commentId)
        );
    }

}
