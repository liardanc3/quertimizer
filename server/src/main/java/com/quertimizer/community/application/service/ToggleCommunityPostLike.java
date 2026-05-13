package com.quertimizer.community.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.community.application.port.in.ToggleCommunityPostLikeUseCase;
import com.quertimizer.community.application.input.ToggleCommunityPostLikeInput;
import com.quertimizer.community.application.output.CommunityReactionOutput;
import com.quertimizer.community.application.port.out.CommunityPostLikeRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostSearchPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ToggleCommunityPostLike implements ToggleCommunityPostLikeUseCase {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityPostLikeRepositoryPort communityPostLikeRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;

    /**
     * 게시글 좋아요 상태를 토글한다.
     *
     * <ol>
     *   <li>게시글 조회
     *   <li>기존 좋아요면 삭제 후 검색 인덱스 동기화
     *   <li>신규 좋아요면 저장, 알람 발행, 검색 인덱스 동기화
     * </ol>
     *
     * @param input 좋아요를 토글할 게시글과 사용자 입력
     */
    @Transactional
    @Override
    @Log("커뮤니티 게시글 좋아요 처리")
    public Optional<CommunityReactionOutput> execute(ToggleCommunityPostLikeInput input) {
        return communityPostRepository.findById(input.getPostId())
                .map(post -> {
                    CommunityPostLikeId postLikeId = new CommunityPostLikeId(input.getPostId(), input.getHandle());
                    if (communityPostLikeRepository.existsById(postLikeId)) {
                        communityPostLikeRepository.deleteById(postLikeId);
                        post.decreaseLikeCount();
                        CommunityPost savedPost = communityPostRepository.save(post);
                        communityPostSearchPort.syncPost(savedPost, communityService.createTags(input.getPostId()));
                        return new CommunityReactionOutput(false, savedPost.getLikeCount());
                    }

                    communityPostLikeRepository.save(CommunityPostLike.create(input.getPostId(), input.getHandle()));
                    post.increaseLikeCount();
                    CommunityPost savedPost = communityPostRepository.save(post);
                    communityPostSearchPort.syncPost(savedPost, communityService.createTags(input.getPostId()));
                    communityService.publishPostLikeAlarm(savedPost, input.getHandle());
                    return new CommunityReactionOutput(true, savedPost.getLikeCount());
                });
    }
}
