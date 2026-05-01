package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.ToggleCommunityCommentLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.service.CommunityService;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityCommentLike {

    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunityService communityService;

    /**
     * 댓글 좋아요 상태를 토글한다.
     *
     * <ol>
     *   <li>댓글 조회
     *   <li>기존 좋아요면 삭제 후 좋아요 수 감소
     *   <li>신규 좋아요면 저장, 좋아요 수 증가, 알람 발행
     * </ol>
     *
     * @param input 좋아요를 토글할 댓글과 사용자 입력
     */
    @Transactional
    public Optional<CommunityReactionOutput> execute(ToggleCommunityCommentLikeInput input) {
        return communityCommentRepository.findById(input.getCommentId())
                .map(comment -> {
                    CommunityCommentLikeId commentLikeId = new CommunityCommentLikeId(input.getCommentId(), input.getHandle());
                    if (communityCommentLikeRepository.existsById(commentLikeId)) {
                        communityCommentLikeRepository.deleteById(commentLikeId);
                        comment.decreaseLikeCount();
                        return new CommunityReactionOutput(false, comment.getLikeCount());
                    }

                    communityCommentLikeRepository.save(CommunityCommentLike.create(input.getCommentId(), input.getHandle()));
                    comment.increaseLikeCount();
                    communityService.publishCommentLikeAlarm(comment, input.getHandle());
                    return new CommunityReactionOutput(true, comment.getLikeCount());
                });
    }
}
