package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.DeleteCommunityPostInput;
import com.quertimizer.community.application.port.CommunityCommentLikeRepository;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostSearchPort;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeleteCommunityPost {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostTagRepository communityPostTagRepository;
    private final CommunityPostSearchPort communityPostSearchPort;

    /**
     * 커뮤니티 게시글을 삭제한다.
     *
     * <ol>
     *   <li>삭제 가능한 게시글 조회
     *   <li>게시글 하위 댓글 좋아요 정리
     *   <li>댓글, 좋아요, 태그, 검색 인덱스 삭제
     * </ol>
     *
     * @param input 삭제할 게시글과 요청자 입력
     */
    @Transactional
    public boolean execute(DeleteCommunityPostInput input) {
        Optional<CommunityPost> post = communityPostRepository.findById(input.getPostId())
                .filter(currentPost -> currentPost.getHandle().equals(input.getHandle()));
        if (post.isEmpty()) {
            return false;
        }

        List<Long> commentIds = communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(input.getPostId()).stream()
                .map(CommunityComment::getCommentId)
                .toList();
        if (!commentIds.isEmpty()) {
            communityCommentLikeRepository.deleteAllByIdCommentIdIn(commentIds);
        }

        communityCommentRepository.deleteAllByPostId(input.getPostId());
        communityPostLikeRepository.deleteAllByIdPostId(input.getPostId());
        communityPostTagRepository.deleteAllByPostId(input.getPostId());
        communityPostRepository.delete(post.get());
        communityPostSearchPort.deletePost(input.getPostId());
        return true;
    }
}
