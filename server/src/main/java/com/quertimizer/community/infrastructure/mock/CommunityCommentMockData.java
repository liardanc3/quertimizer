package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.community.application.port.CommunityCommentRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component("communityCommentMockData")
@DependsOn({"communityPostMockData", "userMockData"})
@RequiredArgsConstructor
public class CommunityCommentMockData {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;

    @PostConstruct
    public void seed() {
        // 기본 댓글 Mock 데이터 적재
        if (!communityCommentRepository.findAllByPostIdOrderByCreatedAtAsc(1L).isEmpty()) {
            return;
        }

        for (CommunityPost post : communityPostRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getPostId))
                .toList()) {
            Optional<Integer> postNumber = resolvePostNumber(post.getPostId());

            if (postNumber.isEmpty()) {
                continue;
            }

            CommunityComment rootComment = communityCommentRepository.save(CommunityComment.create(
                    post.getPostId(),
                    resolveRootCommentHandle(postNumber.get()),
                    null,
                    createRootCommentContent(postNumber.get()),
                    post.getCreatedAt().plusMinutes(25)
            ));
            post.increaseCommentCount();

            if (postNumber.get() <= 10) {
                communityCommentRepository.save(CommunityComment.create(
                        post.getPostId(),
                        resolveReplyCommentHandle(postNumber.get()),
                        rootComment.getCommentId(),
                        createReplyCommentContent(postNumber.get()),
                        post.getCreatedAt().plusMinutes(52)
                ));
                post.increaseCommentCount();
            }

            if (postNumber.get() > 30) {
                for (int commentIndex = 1; commentIndex <= 4; commentIndex++) {
                    communityCommentRepository.save(CommunityComment.create(
                            post.getPostId(),
                            resolveHotCommentHandle(commentIndex),
                            null,
                            createHotCommentContent(postNumber.get(), commentIndex),
                            post.getCreatedAt().plusMinutes(52 + (commentIndex * 17L))
                    ));
                    post.increaseCommentCount();
                }
            }

            communityPostRepository.save(post);
        }
    }

    private String resolveRootCommentHandle(int postNumber) {
        // Root 댓글 Handle 결정
        if (postNumber <= 10) {
            return "advanced%02d".formatted(postNumber);
        }

        if (postNumber <= 20) {
            return "beginner%02d".formatted(postNumber - 10);
        }

        return "intermediate%02d".formatted(toExistingUserNumber(postNumber));
    }

    private String resolveReplyCommentHandle(int postNumber) {
        // Reply 댓글 Handle 결정
        return "intermediate%02d".formatted(postNumber);
    }

    private String resolveHotCommentHandle(int commentIndex) {
        // 인기 댓글 Handle 결정
        return switch (commentIndex) {
            case 1 -> "liardanc3";
            case 2 -> "advanced08";
            case 3 -> "intermediate03";
            default -> "beginner04";
        };
    }

    private String createRootCommentContent(int postNumber) {
        // Root 댓글 본문 생성
        return "comment-seed-root-%02d / 필터 위치를 orders에 먼저 두면 읽기가 더 편했다는 점에는 동의한다.".formatted(postNumber);
    }

    private String createReplyCommentContent(int postNumber) {
        // Reply 댓글 본문 생성
        return "comment-seed-reply-%02d / COUNT DISTINCT를 같이 두고 검증하면 중복 집계 여부를 더 빨리 찾을 수 있었다.".formatted(postNumber);
    }

    private String createHotCommentContent(int postNumber, int commentIndex) {
        // 인기 댓글 본문 생성
        return "comment-seed-hot-%02d-%02d / 본문이 길어서 대시보드 미리보기와 상세 본문 기준을 같이 확인하기 좋다."
                .formatted(postNumber, commentIndex);
    }

    private Optional<Integer> resolvePostNumber(Long postId) {
        // Seed 게시글 번호 결정
        return CommunityPostIdPolicy.resolveSeedPostNumber(postId);
    }

    private int toExistingUserNumber(int postNumber) {
        // 기존 사용자 번호 변환
        return ((postNumber - 1) % 10) + 1;
    }
}
