package com.quertimizer.mock;

import com.quertimizer.entity.CommunityPost;
import com.quertimizer.entity.CommunityPostLike;
import com.quertimizer.repository.CommunityPostLikeRepository;
import com.quertimizer.repository.CommunityPostRepository;
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
        List<CommunityPostLike> postLikes = new ArrayList<>();

        for (CommunityPost post : communityPostRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getPostId))
                .toList()) {
            int postNumber = resolvePostNumber(post.getPostId());

            for (String likerUserId : resolveLikerUserIds(postNumber)) {
                postLikes.add(CommunityPostLike.create(post.getPostId(), likerUserId));
                post.increaseLikeCount();
            }

            communityPostRepository.save(post);
        }

        communityPostLikeRepository.saveAll(postLikes);
    }

    private List<String> resolveLikerUserIds(int postNumber) {
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
        return Integer.parseInt(postId.substring(postId.length() - 2));
    }
}
