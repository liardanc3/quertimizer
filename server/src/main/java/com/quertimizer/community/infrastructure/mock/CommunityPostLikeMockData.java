package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.application.port.CommunityPostLikeRepository;
import com.quertimizer.community.application.port.CommunityPostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component("communityPostLikeMockData")
@DependsOn({"communityPostMockData", "userMockData"})
@RequiredArgsConstructor
public class CommunityPostLikeMockData {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;

    @PostConstruct
    public void seed() {
        // 기본 게시글 좋아요 Mock 데이터 적재
        List<CommunityPostLike> postLikes = new ArrayList<>();

        for (CommunityPost post : communityPostRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getPostId))
                .toList()) {
            int postNumber = resolvePostNumber(post.getPostId());

            for (String likerHandle : resolveLikerHandles(postNumber)) {
                postLikes.add(CommunityPostLike.create(post.getPostId(), likerHandle));
                post.increaseLikeCount();
            }

            communityPostRepository.save(post);
        }

        communityPostLikeRepository.saveAll(postLikes);
    }

    private List<String> resolveLikerHandles(int postNumber) {
        // Liker Handles 결정
        if (postNumber > 30) {
            return List.of(
                    "liardanc3",
                    "admin",
                    "problemgen01",
                    "beginner01",
                    "beginner04",
                    "intermediate03",
                    "intermediate07",
                    "advanced03",
                    "advanced08"
            );
        }

        if (postNumber <= 10) {
            return List.of(
                    "intermediate%02d".formatted(postNumber),
                    "advanced%02d".formatted(postNumber),
                    "liardanc3"
            );
        }

        if (postNumber <= 20) {
            return List.of(
                    "beginner%02d".formatted(postNumber - 10),
                    "advanced%02d".formatted(postNumber - 10)
            );
        }

        return List.of("beginner%02d".formatted(postNumber - 20));
    }

    private int resolvePostNumber(String postId) {
        // 게시글 번호 결정
        return Integer.parseInt(postId.substring(postId.length() - 2));
    }
}
