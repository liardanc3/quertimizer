package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component("communityPostTagMockData")
@DependsOn("communityPostMockData")
@RequiredArgsConstructor
public class CommunityPostTagMockData {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;

    @PostConstruct
    public void seed() {
        // 기본 게시글 태그 Mock 데이터 적재
        List<CommunityPostTag> postTags = new ArrayList<>();

        for (CommunityPost post : communityPostRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getPostId))
                .toList()) {
            List<String> tags = createTags(resolvePostNumber(post.getPostId()));

            for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
                postTags.add(CommunityPostTag.create(post.getPostId(), tags.get(tagIndex), tagIndex + 1));
            }
        }

        communityPostTagRepository.saveAll(postTags);
    }

    private List<String> createTags(int postNumber) {
        // 태그 목록 생성
        if (postNumber <= 15) {
            return List.of(
                    "00001-00001",
                    postNumber % 2 == 0 ? "postgresql" : "oracle",
                    "sql",
                    postNumber % 3 == 0 ? "group-by" : "join",
                    postNumber % 4 == 0 ? "execution-plan" : "aggregate"
            );
        }

        return switch (postNumber) {
            case 16 -> List.of("postgresql", "group-by", "성능");
            case 17 -> List.of("oracle", "execution-plan", "질문");
            case 18 -> List.of("index", "postgresql", "질문");
            case 19 -> List.of("aggregate", "having", "sql");
            case 20 -> List.of("sort", "tuning", "질문");
            case 21 -> List.of("join", "sql", "정리");
            case 22 -> List.of("execution-plan", "tuning", "질문");
            case 23 -> List.of("explain-analyze", "입문", "sql");
            case 24 -> List.of("order-by", "index", "postgresql");
            case 25 -> List.of("count-distinct", "aggregate", "질문");
            case 26 -> List.of("join", "type-cast", "database");
            case 27 -> List.of("subquery", "join", "sql");
            case 28 -> List.of("index", "benchmark", "tuning");
            case 29 -> List.of("execution-plan", "study", "database");
            default -> List.of("sample-data", "validation", "sql");
        };
    }

    private int resolvePostNumber(String postId) {
        // 게시글 번호 결정
        return Integer.parseInt(postId.substring(postId.length() - 2));
    }
}
