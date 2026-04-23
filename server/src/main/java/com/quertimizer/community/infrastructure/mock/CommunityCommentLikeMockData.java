package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityComment;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.infrastructure.repository.CommunityCommentLikeRepository;
import com.quertimizer.community.infrastructure.repository.CommunityCommentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component("communityCommentLikeMockData")
@DependsOn({"communityCommentMockData", "userMockData"})
@RequiredArgsConstructor
public class CommunityCommentLikeMockData {

    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityCommentLikeRepository communityCommentLikeRepository;

    @PostConstruct
    public void seed() {
        List<CommunityCommentLike> commentLikes = new ArrayList<>();

        for (CommunityComment comment : communityCommentRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityComment::getCommentId))
                .toList()) {
            int commentNumber = comment.getCommentId().intValue();

            commentLikes.add(CommunityCommentLike.create(comment.getCommentId(), "liardanc3"));
            comment.increaseLikeCount();

            if (commentNumber % 3 == 0) {
                commentLikes.add(CommunityCommentLike.create(comment.getCommentId(), "admin"));
                comment.increaseLikeCount();
            }

            communityCommentRepository.save(comment);
        }

        communityCommentLikeRepository.saveAll(commentLikes);
    }
}
