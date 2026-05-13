package com.quertimizer.community.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.community.application.port.in.GetCommunityPostDetailUseCase;
import com.quertimizer.community.application.input.CommunityPostDetailInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.output.CommunityPostDetailOutput;
import com.quertimizer.community.application.port.out.CommunityCommentRepositoryPort;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.community.domain.policy.CommunityViewPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetCommunityPostDetail implements GetCommunityPostDetailUseCase {

    private final CommunityPostRepositoryPort communityPostRepository;
    private final CommunityCommentRepositoryPort communityCommentRepository;
    private final CommunityService communityService;
    private final CommunityViewPolicy communityViewPolicy;

    /**
     * 게시글 상세를 현재 사용자 반응 정보와 함께 조회한다.
     *
     * <ol>
     *   <li>게시글 조회와 조회수 증가
     *   <li>태그, 댓글, 좋아요 상태 조회
     *   <li>게시글 상세 응답 조립
     * </ol>
     *
     * @param input 조회할 게시글과 현재 사용자 입력
     */
    @Transactional
    @Override
    @Log("커뮤니티 게시글 상세 조회")
    public Optional<CommunityPostDetailOutput> execute(CommunityPostDetailInput input) {
        return communityPostRepository.findById(input.getPostId())
                .map(post -> {
                    CommunityPost resolvedPost = post;
                    if (communityViewPolicy.shouldIncreaseViewCount(input.getPostId(), input.getViewerKey())) {
                        post.increaseViewCount();
                        resolvedPost = communityPostRepository.save(post);
                    }

                    List<String> tags = communityService.createTags(input.getPostId());
                    List<CommunityComment> comments = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(input.getPostId());
                    Map<Long, Boolean> likedCommentById = communityService.createLikedCommentById(comments, input.getCurrentHandle());
                    List<CommunityCommentOutput> commentTree = communityService.createCommentTree(comments, likedCommentById);
                    return new CommunityPostDetailOutput(
                            CommunityPostIdPolicy.format(resolvedPost.getPostId()),
                            resolvedPost.getTitle(), resolvedPost.getHandle(), resolvedPost.getContentJson(),
                            communityService.createImageIds(resolvedPost.getImageIds()), tags, communityService.resolveCategory(resolvedPost),
                            resolvedPost.getCreatedAt(), resolvedPost.getUpdatedAt(), resolvedPost.getViewCount(),
                            resolvedPost.getLikeCount(), resolvedPost.getCommentCount(),
                            communityService.isPostLiked(input.getPostId(), input.getCurrentHandle()),
                            input.getCurrentHandle() != null && input.getCurrentHandle().equals(resolvedPost.getHandle()),
                            commentTree
                    );
                });
    }
}
