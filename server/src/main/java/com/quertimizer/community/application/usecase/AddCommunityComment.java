package com.quertimizer.community.application.usecase;

import com.quertimizer.community.application.input.AddCommunityCommentInput;
import com.quertimizer.community.application.output.CommunityCommentOutput;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostSearchPort;
import com.quertimizer.community.application.service.CommunityService;
import com.quertimizer.community.domain.entity.CommunityComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddCommunityComment {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostSearchPort communityPostSearchPort;
    private final CommunityService communityService;

    /**
     * 게시글 댓글을 추가한다.
     *
     * <ol>
     *   <li>게시글과 부모 댓글 조회
     *   <li>댓글 저장과 게시글 댓글 수 증가
     *   <li>검색 인덱스 동기화와 댓글 알람 발행
     * </ol>
     *
     * @param input 댓글을 추가할 게시글, 작성자, 댓글 내용 입력
     */
    @Transactional
    public Optional<CommunityCommentOutput> execute(AddCommunityCommentInput input) {
        return communityPostRepository.findById(input.getPostId())
                .map(post -> {
                    Optional<CommunityComment> parentComment = Optional.ofNullable(input.getComment().getParentCommentId())
                            .flatMap(communityCommentRepository::findById)
                            .filter(currentComment -> currentComment.getPostId().equals(input.getPostId()));
                    CommunityComment comment = communityCommentRepository.save(CommunityComment.create(
                            input.getPostId(), input.getHandle(),
                            input.getComment().getParentCommentId(), input.getComment().getContent().trim()
                    ));

                    post.increaseCommentCount();
                    communityPostSearchPort.syncPost(post, communityService.createTags(input.getPostId()));
                    communityService.publishCommentAlarms(post, comment, parentComment, input.getHandle());
                    return new CommunityCommentOutput(
                            comment.getCommentId(), comment.getHandle(), comment.getContent(),
                            comment.getCreatedAt(), comment.getLikeCount(), false, List.of()
                    );
                });
    }
}
