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

    private static final String[] HOT_POST_TITLES = {
            "실행 계획 바뀐 뒤 Cost는 내려갔는데 실제 체감은 달랐던 후기",
            "인덱스 하나 추가하고 JOIN 순서를 다시 본 전체 과정 정리",
            "대시보드 미리보기 확인용으로 길게 적어본 SQL 튜닝 회고",
            "PostgreSQL과 Oracle에서 같은 문제를 풀 때 다르게 봐야 했던 부분"
    };

    private final CommunityPostRepository communityPostRepository;

    @PostConstruct
    public void seed() {
        communityPostRepository.saveAll(createPosts());
    }

    private List<CommunityPost> createPosts() {
        List<CommunityPost> posts = new ArrayList<>();

        for (int postNumber = 1; postNumber <= 34; postNumber++) {
            CommunityPost post = CommunityPost.create(
                    createPostId(postNumber),
                    createAuthorId(postNumber),
                    createTitle(postNumber),
                    createContentHtml(postNumber),
                    createContentText(postNumber),
                    LocalDateTime.of(2026, 4, 1, 9, 0).plusHours(postNumber * 4L)
            );

            for (int count = 0; count < createInitialViewCount(postNumber); count++) {
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
        if (postNumber > 30) {
            return switch (postNumber) {
                case 31 -> "liardanc3";
                case 32 -> "advanced03";
                case 33 -> "intermediate07";
                default -> "beginner04";
            };
        }

        if (postNumber <= 10) {
            return "beginner%02d".formatted(postNumber);
        }

        if (postNumber <= 20) {
            return "intermediate%02d".formatted(postNumber - 10);
        }

        return "advanced%02d".formatted(postNumber - 20);
    }

    private String createTitle(int postNumber) {
        if (postNumber > 30) {
            return HOT_POST_TITLES[postNumber - 31];
        }

        return postNumber <= 15
                ? PROBLEM_POST_TITLES[postNumber - 1]
                : GENERAL_POST_TITLES[postNumber - 16];
    }

    private String createContentHtml(int postNumber) {
        if (postNumber > 30) {
            return createHotContentHtml(postNumber);
        }

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
        if (postNumber > 30) {
            return createHotContentText(postNumber);
        }

        if (postNumber <= 15) {
            return "%s / 주문 기간 조건은 orders에 먼저 두고 COUNT DISTINCT와 SUM 기준을 함께 확인했다."
                    .formatted(createTitle(postNumber));
        }

        return "%s / 실행 시간만 보지 말고 조인 방식, 정렬, 집계 노드를 같이 보는 기준을 정리했다."
                .formatted(createTitle(postNumber));
    }

    private int createInitialViewCount(int postNumber) {
        if (postNumber > 30) {
            return switch (postNumber) {
                case 31 -> 7200;
                case 32 -> 6400;
                case 33 -> 5800;
                default -> 5200;
            };
        }

        return 12 + (postNumber * 3);
    }

    private String createHotContentHtml(int postNumber) {
        return switch (postNumber) {
            case 31 -> """
                    <h2>%s</h2>
                    <p>처음에는 Cost가 내려가면 무조건 좋은 방향이라고 생각했는데, 실제로 여러 번 실행해 보니 버퍼 접근 패턴과 정렬 위치가 같이 바뀌면서 체감 시간이 생각보다 크게 달라졌다.</p>
                    <p>특히 WHERE 조건을 먼저 좁히는 쿼리와 JOIN 이후에 필터가 적용되는 쿼리를 나란히 비교했을 때, 실행 계획에서는 작은 차이처럼 보이던 부분이 데이터가 늘어날수록 훨씬 크게 벌어졌다.</p>
                    <p>이번에는 단순히 빠른 SQL 하나만 남기지 않고, 왜 그런 계획이 나왔는지와 어떤 조건에서 다시 느려질 수 있는지를 같이 적어 두었다. 대시보드 미리보기에서도 본문 일부가 충분히 보여야 해서 문단을 길게 유지했다.</p>
                    <blockquote>Cost, Rows, Scan 방식은 따로 보지 말고 같은 변경 전후 묶음으로 비교해야 원인을 놓치지 않는다.</blockquote>
                    """.formatted(createTitle(postNumber));
            case 32 -> """
                    <h2>%s</h2>
                    <p>customers.region 조건을 기준으로 인덱스를 추가한 뒤 바로 SELECT만 다시 실행했을 때는 좋아 보였지만, 실제 제출 흐름에서는 DDL 반영과 SELECT 검증, 실행 계획 분석이 순서대로 이어지기 때문에 중간 결과를 따로 봐야 했다.</p>
                    <p>JOIN 순서는 옵티마이저가 알아서 잡아준다고 생각하기 쉽지만, 통계가 부족하거나 조건 선택도가 애매하면 예상과 다른 테이블을 먼저 읽기도 했다. 그래서 같은 SQL을 조금씩 나눠 실행하면서 어느 단계에서 Rows가 커지는지 확인했다.</p>
                    <p>결론적으로 인덱스 추가 자체보다 그 인덱스가 실제 필터 조건에 쓰이는지, 그리고 JOIN 결과를 줄이는 위치에 쓰이는지가 더 중요했다. 이 글은 그 과정을 단계별로 적어 둔 기록이다.</p>
                    <blockquote>인덱스가 존재한다는 사실보다 실행 계획에서 어떤 노드가 그 인덱스를 선택했는지가 더 중요하다.</blockquote>
                    """.formatted(createTitle(postNumber));
            case 33 -> """
                    <h2>%s</h2>
                    <p>대시보드 카드에서 본문 미리보기가 실제 게시글 본문과 다르게 보이는지 확인하려고 제목과 태그를 제외한 본문 문장을 의도적으로 길게 작성했다. 제목 블록은 상세 화면에서 제거되므로 미리보기에서도 같은 기준이 적용되어야 한다.</p>
                    <p>중간에 이미지를 붙여 넣은 경우에는 미리보기에서 이미지 자체를 보여주기보다 [이미지] 같은 텍스트로 대체하는 편이 카드 높이를 안정적으로 유지하기 좋았다. 긴 문단은 일정 줄 수 이후 말줄임 처리되어야 하고, 다음 페이지 카드가 비어 있어도 영역이 늘어나면 안 된다.</p>
                    <p>따라서 이 Mock Data는 조회수와 반응 수를 크게 잡고, 본문 길이도 충분히 길게 잡아 캐러셀 두 번째 페이지와 세 번째 페이지의 2x2 고정 레이아웃을 확인하는 데 쓰기 위한 데이터다.</p>
                    <blockquote>미리보기는 제목이 아니라 사용자가 실제로 읽을 본문에서 시작해야 한다.</blockquote>
                    """.formatted(createTitle(postNumber));
            default -> """
                    <h2>%s</h2>
                    <p>PostgreSQL에서는 EXPLAIN ANALYZE로 실제 실행 결과를 바로 확인하는 흐름이 자연스러웠고, Oracle에서는 같은 문제라도 실행 계획에서 보는 용어와 비용 해석 방식이 달라서 비교 기준을 따로 잡아야 했다.</p>
                    <p>같은 SELECT라도 DBMS에 따라 선택되는 Scan, Join, Sort 요소가 달라질 수 있으니 문제 목록이나 제출 목록에서 현재 선택한 DBMS 기준으로만 실행 계획 요소를 보는 것이 훨씬 읽기 좋았다.</p>
                    <p>커뮤니티 글에서는 이런 차이를 짧게만 적으면 맥락이 부족해져서, 어떤 상황에서 다른 계획이 나왔고 어떤 지표를 먼저 봤는지까지 함께 남겨두는 편이 더 도움이 됐다.</p>
                    <blockquote>DBMS별 차이를 비교할 때는 같은 용어처럼 보여도 실제 의미와 평가 기준을 다시 확인해야 한다.</blockquote>
                    """.formatted(createTitle(postNumber));
        };
    }

    private String createHotContentText(int postNumber) {
        return switch (postNumber) {
            case 31 -> """
                    처음에는 Cost가 내려가면 무조건 좋은 방향이라고 생각했지만, 실제로 여러 번 실행해 보니 버퍼 접근 패턴과 정렬 위치가 같이 바뀌면서 체감 시간이 달라졌다. WHERE 조건을 먼저 좁히는 쿼리와 JOIN 이후에 필터가 적용되는 쿼리를 나란히 비교했고, 데이터가 늘어날수록 작은 계획 차이가 크게 벌어지는 과정을 정리했다.
                    """.trim();
            case 32 -> """
                    customers.region 조건을 기준으로 인덱스를 추가한 뒤 DDL 반영, SELECT 검증, 실행 계획 분석을 순서대로 확인했다. 인덱스가 존재한다는 사실보다 실제 필터 조건에 쓰이는지와 JOIN 결과를 줄이는 위치에 쓰이는지가 중요하다는 점을 단계별로 기록했다.
                    """.trim();
            case 33 -> """
                    대시보드 카드에서 본문 미리보기가 실제 게시글 본문과 같은 기준으로 보이는지 확인하려고 작성한 긴 Mock Data다. 제목 블록은 상세 화면에서 제거되므로 미리보기에서도 같은 기준이 적용되어야 하고, 이미지나 긴 문단은 카드 높이를 해치지 않게 처리되어야 한다.
                    """.trim();
            default -> """
                    PostgreSQL과 Oracle에서 같은 문제를 풀 때 실행 계획 용어와 비용 해석 방식이 달라지는 부분을 정리했다. 같은 SELECT라도 DBMS에 따라 Scan, Join, Sort 요소가 달라질 수 있으므로 현재 선택한 DBMS 기준으로 계획 요소를 보는 것이 더 읽기 좋았다.
                    """.trim();
        };
    }
}
