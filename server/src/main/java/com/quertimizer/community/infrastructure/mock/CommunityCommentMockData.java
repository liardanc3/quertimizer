package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.infrastructure.repository.CommunityCommentRepository;
import com.quertimizer.community.infrastructure.repository.CommunityPostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;

@Component("communityCommentMockData")
@DependsOn({"communityPostMockData", "userMockData"})
@RequiredArgsConstructor
public class CommunityCommentMockData {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;

    @PostConstruct
    public void seed() {
        for (CommunityPost post : communityPostRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getPostId))
                .toList()) {
            int postNumber = resolvePostNumber(post.getPostId());

            CommunityComment rootComment = communityCommentRepository.save(CommunityComment.create(
                    post.getPostId(),
                    resolveRootCommentHandle(postNumber),
                    null,
                    createRootCommentContent(postNumber),
                    post.getCreatedAt().plusMinutes(25)
            ));
            post.increaseCommentCount();

            if (postNumber <= 10) {
                communityCommentRepository.save(CommunityComment.create(
                        post.getPostId(),
                        resolveReplyCommentHandle(postNumber),
                        rootComment.getCommentId(),
                        createReplyCommentContent(postNumber),
                        post.getCreatedAt().plusMinutes(52)
                ));
                post.increaseCommentCount();
            }

            communityPostRepository.save(post);
        }
    }

    private String resolveRootCommentHandle(int postNumber) {
        if (postNumber <= 10) {
            return "advanced%02d".formatted(postNumber);
        }

        if (postNumber <= 20) {
            return "beginner%02d".formatted(postNumber - 10);
        }

        return "intermediate%02d".formatted(postNumber - 20);
    }

    private String resolveReplyCommentHandle(int postNumber) {
        return "intermediate%02d".formatted(postNumber);
    }

    private String createRootCommentContent(int postNumber) {
        return "comment-seed-root-%02d / 필터 위치를 orders에 먼저 두면 읽기가 더 편했다는 점에는 동의한다.".formatted(postNumber);
    }

    private String createReplyCommentContent(int postNumber) {
        return "comment-seed-reply-%02d / COUNT DISTINCT를 같이 두고 검증하면 중복 집계 여부를 더 빨리 찾을 수 있었다.".formatted(postNumber);
    }

    private int resolvePostNumber(String postId) {
        return Integer.parseInt(postId.substring(postId.length() - 2));
    }
}
