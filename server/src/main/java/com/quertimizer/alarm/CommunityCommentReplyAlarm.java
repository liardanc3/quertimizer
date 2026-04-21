package com.quertimizer.alarm;

import java.util.Map;

public record CommunityCommentReplyAlarm(String recipientUserId,
                                         String actorUserId,
                                         String postId,
                                         String replyContent,
                                         Long replyCommentId) implements AlarmSpec {

    @Override
    public String alarmType() {
        return "REPLY_MY_COMMENT";
    }

    @Override
    public String title() {
        return "새 대댓글";
    }

    @Override
    public String message() {
        return actorUserId + "님이 댓글에 대댓글을 남겼다.";
    }

    @Override
    public AlarmTarget target() {
        return AlarmTarget.of("/community/" + postId, "#community-comment-" + replyCommentId);
    }

    @Override
    public Map<String, AlarmBinding> bindings() {
        return Map.of(
                "handle", AlarmBinding.of(actorUserId, "/profile/" + actorUserId),
                "comment", AlarmBinding.of(replyContent, "/community/" + postId, "#community-comment-" + replyCommentId)
        );
    }

}
