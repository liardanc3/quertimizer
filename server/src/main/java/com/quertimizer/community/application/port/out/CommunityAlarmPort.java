package com.quertimizer.community.application.port.out;

public interface CommunityAlarmPort {

    void publishPostLike(String recipientHandle, String actorHandle, String postId, String postTitle);

    void publishPostComment(String recipientHandle, String actorHandle, String postId, String postTitle, Long commentId);

    void publishCommentLike(String recipientHandle, String actorHandle, String postId, String commentContent, Long commentId);

    void publishCommentReply(String recipientHandle, String actorHandle, String postId,
                             String parentCommentContent, Long parentCommentId, Long replyCommentId);

}
