package com.quertimizer.alarm;

import java.util.Map;

public record CommunityCommentLikeAlarm(String recipientUserId,
                                        String actorUserId,
                                        String postId,
                                        String commentContent,
                                        Long commentId) implements AlarmSpec {

    @Override
    public String alarmType() {
        return "LIKE_MY_COMMENT";
    }

    @Override
    public String title() {
        return "댓글 좋아요";
    }

    @Override
    public String message() {
        return actorUserId + "님이 댓글에 좋아요를 눌렀다.";
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
