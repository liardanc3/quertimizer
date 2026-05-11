package com.quertimizer.community.adapter.out.alarm;

import com.quertimizer.alarm.application.port.in.PublishAlarmUseCase;
import com.quertimizer.alarm.domain.model.CommunityCommentLikeAlarm;
import com.quertimizer.alarm.domain.model.CommunityCommentReplyAlarm;
import com.quertimizer.alarm.domain.model.CommunityPostCommentAlarm;
import com.quertimizer.alarm.domain.model.CommunityPostLikeAlarm;
import com.quertimizer.community.application.port.out.CommunityAlarmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityAlarmAdapter implements CommunityAlarmPort {

    private final PublishAlarmUseCase publishAlarm;

    @Override
    public void publishPostLike(String recipientHandle, String actorHandle, String postId, String postTitle) {
        // 게시글 좋아요 알람 명세 생성 후 alarm use case 호출
        publishAlarm.execute(new CommunityPostLikeAlarm(recipientHandle, actorHandle, postId, postTitle));
    }

    @Override
    public void publishPostComment(String recipientHandle, String actorHandle, String postId, String postTitle, Long commentId) {
        // 게시글 댓글 알람 명세 생성 후 alarm use case 호출
        publishAlarm.execute(new CommunityPostCommentAlarm(recipientHandle, actorHandle, postId, postTitle, commentId));
    }

    @Override
    public void publishCommentLike(String recipientHandle, String actorHandle, String postId,
                                   String commentContent, Long commentId) {
        // 댓글 좋아요 알람 명세 생성 후 alarm use case 호출
        publishAlarm.execute(new CommunityCommentLikeAlarm(recipientHandle, actorHandle, postId, commentContent, commentId));
    }

    @Override
    public void publishCommentReply(String recipientHandle, String actorHandle, String postId,
                                    String replyContent, Long replyCommentId) {
        // 대댓글 알람 명세 생성 후 alarm use case 호출
        publishAlarm.execute(new CommunityCommentReplyAlarm(recipientHandle, actorHandle, postId, replyContent, replyCommentId));
    }

}
