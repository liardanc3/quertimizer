package com.quertimizer.community.infrastructure.mock;

import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.infrastructure.repository.CommunityPostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component("communityPostMockData")
@DependsOn("userMockData")
@RequiredArgsConstructor
public class CommunityPostMockData {

    private static final String[] PROBLEM_POST_TITLES = {
            "00001-00001 첫 풀이에서 GROUP BY를 어디까지 써야 하나요?",
            "00001-00001 COUNT DISTINCT가 필요한 이유 정리",
            "00001-00001 JOIN 순서를 바꾸니 더 읽기 쉬워졌습니다",
            "00001-00001 WHERE 조건을 주문 테이블에만 둔 이유",
            "00001-00001 초보자 풀이 공유: 먼저 정답부터 맞춘 버전",
            "00001-00001 SUM 계산이 두 배로 나왔던 원인",
            "00001-00001 Oracle에서 주문 금액 집계할 때 주의한 점",
            "00001-00001 PostgreSQL 실행 계획 보고 수정한 부분",
            "00001-00001 GROUP BY 컬럼을 최소화한 버전",
            "00001-00001 고객 없는 주문은 없다고 가정해도 될까요?",
            "00001-00001 해시 조인보다 중첩 루프가 빨랐던 사례",
            "00001-00001 인덱스 없이도 먼저 풀어본 정리",
            "00001-00001 HAVING 없이 푸는 쪽이 더 단순했습니다",
            "00001-00001 주문 기간 필터를 실수했던 부분 정리",
            "00001-00001 풀이 비교: COUNT와 SUM 위치 차이"
    };

    private static final String[] GENERAL_POST_TITLES = {
            "PostgreSQL에서 GROUP BY 성능 볼 때 먼저 확인하는 것들",
            "Oracle 실행 계획에서 HASH JOIN 해석이 헷갈립니다",
            "INDEX SCAN과 INDEX ONLY SCAN 차이를 쉽게 설명해 주세요",
            "집계 쿼리에서 WHERE와 HAVING을 나누는 기준",
            "정렬이 많은 쿼리에서 SORT 비용을 줄이는 방법이 있나요?",
            "LEFT JOIN을 INNER JOIN으로 바꿔도 되는 조건 정리",
            "실행 시간이 비슷한데 계획이 다른 경우 무엇을 믿어야 하나요?",
            "DB 초보가 EXPLAIN ANALYZE 읽을 때 순서",
            "ORDER BY가 느릴 때 인덱스 설계 체크리스트",
            "COUNT DISTINCT가 느릴 때 대체 전략이 있을까요?",
            "JOIN 컬럼 타입이 다르면 성능에 영향이 큰가요?",
            "서브쿼리보다 조인이 더 좋은 경우는 언제인가요?",
            "인덱스 추가 전후 비교할 때 공정하게 보는 방법",
            "실행 계획 요소를 공부할 때 추천하는 순서",
            "대량 데이터에서 샘플 쿼리로 먼저 검증하는 습관"
    };

    private final CommunityPostRepository communityPostRepository;

    @PostConstruct
    public void seed() {
        communityPostRepository.saveAll(createPosts());
    }

    private List<CommunityPost> createPosts() {
        List<CommunityPost> posts = new ArrayList<>();

        for (int postNumber = 1; postNumber <= 30; postNumber++) {
            CommunityPost post = CommunityPost.create(
                    createPostId(postNumber),
                    createAuthorId(postNumber),
                    createTitle(postNumber),
                    createContentHtml(postNumber),
                    createContentText(postNumber),
                    LocalDateTime.of(2026, 4, 1, 9, 0).plusHours(postNumber * 4L)
            );

            for (int count = 0; count < 12 + (postNumber * 3); count++) {
                post.increaseViewCount();
            }

            posts.add(post);
        }

        return posts;
    }

    private String createPostId(int postNumber) {
        return "community-seed-%02d".formatted(postNumber);
    }

    private String createAuthorId(int postNumber) {
        if (postNumber <= 10) {
            return "beginner%02d".formatted(postNumber);
        }

        if (postNumber <= 20) {
            return "intermediate%02d".formatted(postNumber - 10);
        }

        return "advanced%02d".formatted(postNumber - 20);
    }

    private String createTitle(int postNumber) {
        return postNumber <= 15
                ? PROBLEM_POST_TITLES[postNumber - 1]
                : GENERAL_POST_TITLES[postNumber - 16];
    }

    private String createContentHtml(int postNumber) {
        if (postNumber <= 15) {
            return """
                    <h2>%s</h2>
                    <p>00001-00001을 풀면서 주문 기간 조건은 <b>orders</b>에 먼저 두고, 그 다음에 customers와 order_items를 조인했다.</p>
                    <p>핵심은 주문 건수와 금액 집계를 분리해서 검증하는 것이었다. COUNT DISTINCT와 SUM 위치를 함께 보면 실수 줄이기가 쉬웠다.</p>
                    <blockquote>COUNT(DISTINCT o.order_id)와 SUM(oi.quantity * oi.unit_price)를 같이 두면 중복 집계를 확인하기 좋다.</blockquote>
                    """.formatted(createTitle(postNumber));
        }

        return """
                <h2>%s</h2>
                <p>최근 비슷한 쿼리를 보면서 정리한 메모다. 실행 시간 하나만 보지 말고 조인 방식, 정렬, 집계 노드를 같이 보는 편이 낫다고 느꼈다.</p>
                <p><b>요점</b>은 작은 데이터에서 빠른 쿼리가 큰 데이터에서도 항상 빠르지 않다는 점이다.</p>
                <blockquote>정답 SQL과 튜닝 SQL은 분리해서 비교해야 원인을 더 잘 볼 수 있다.</blockquote>
                """.formatted(createTitle(postNumber));
    }

    private String createContentText(int postNumber) {
        if (postNumber <= 15) {
            return "%s / 주문 기간 조건은 orders에 먼저 두고 COUNT DISTINCT와 SUM 기준을 함께 확인했다."
                    .formatted(createTitle(postNumber));
        }

        return "%s / 실행 시간만 보지 말고 조인 방식, 정렬, 집계 노드를 같이 보는 기준을 정리했다."
                .formatted(createTitle(postNumber));
    }
}
