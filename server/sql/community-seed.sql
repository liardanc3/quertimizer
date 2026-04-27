CREATE TEMP TABLE tmp_community_user_seed AS
SELECT
    user_seq,
    CASE
        WHEN user_seq <= 10 THEN 'beginner' || LPAD(user_seq::text, 2, '0')
        WHEN user_seq <= 20 THEN 'intermediate' || LPAD((user_seq - 10)::text, 2, '0')
        ELSE 'advanced' || LPAD((user_seq - 20)::text, 2, '0')
    END AS user_id,
    CASE
        WHEN user_seq <= 10 THEN '초보자'
        WHEN user_seq <= 20 THEN '중급자'
        ELSE '상급자'
    END AS skill_label,
    CASE
        WHEN user_seq % 2 = 0 THEN 'POSTGRESQL'
        ELSE 'MYSQL'
    END AS default_dbms
FROM generate_series(1, 30) AS user_seq;

INSERT INTO quertimizer."user" (
    user_id,
    password,
    email,
    bio,
    default_dbms,
    sql_public,
    execution_percentile_public,
    solved_records_public,
    solved_problem_count_public,
    solved_problem_count,
    solved_execution_time_sum_ms,
    signup_at
)
SELECT
    users.user_id,
    repeat('a', 128),
    users.user_id || '@example.com',
    users.skill_label || ' SQL 학습과 정리를 꾸준히 올리는 테스트 계정',
    users.default_dbms,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    0,
    0,
    TIMESTAMP '2026-03-01 09:00:00' + (users.user_seq * INTERVAL '2 hour')
FROM tmp_community_user_seed users
ON CONFLICT (user_id) DO UPDATE
SET
    email = EXCLUDED.email,
    bio = EXCLUDED.bio,
    default_dbms = EXCLUDED.default_dbms,
    execution_percentile_public = EXCLUDED.execution_percentile_public,
    solved_records_public = EXCLUDED.solved_records_public,
    solved_problem_count_public = EXCLUDED.solved_problem_count_public;

CREATE TEMP TABLE tmp_community_post_seed (
    post_seq INTEGER PRIMARY KEY,
    post_id VARCHAR(50) NOT NULL,
    user_seq INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    detail TEXT NOT NULL,
    quote_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    tags TEXT[] NOT NULL
);

INSERT INTO tmp_community_post_seed (
    post_seq,
    post_id,
    user_seq,
    title,
    summary,
    detail,
    quote_text,
    created_at,
    tags
)
VALUES
(1, 'community-seed-0001', 1, '00001-00001 처음 풀면서 헷갈렸던 부분 정리', '기간 조건을 먼저 고정하고 주문 수와 금액 집계를 분리해서 봤다.', '처음에는 order_items 때문에 주문 수가 늘어났고 COUNT(DISTINCT o.order_id)로 기준을 다시 잡았다.', '정답보다 먼저 중복이 왜 생겼는지 확인하는 게 중요했다.', TIMESTAMP '2026-03-02 09:10:00', ARRAY['00001-00001', '문제풀이', '집계', '초보자']),
(2, 'community-seed-0002', 2, '00001-00001 COUNT(DISTINCT) 꼭 써야 하는 이유', '주문 수를 세는 기준이 order_items 행 수가 아니라 order_id라는 점을 다시 확인했다.', 'SUM은 상품 행 기준으로 계산하되 주문 건수는 DISTINCT 없이 맞지 않았다.', '집계 컬럼마다 기준 단위가 다를 수 있다는 걸 이 문제에서 처음 체감했다.', TIMESTAMP '2026-03-02 10:25:00', ARRAY['00001-00001', '문제풀이', 'COUNT', '초보자']),
(3, 'community-seed-0003', 3, '00001-00001 JOIN 순서보다 기간 조건이 먼저였음', '어떤 JOIN을 쓰는지보다 먼저 3월 범위를 정확히 자르는 게 중요했다.', '조건을 orders에 먼저 걸어 둔 뒤 customers와 order_items를 연결하니 결과 검증이 쉬워졌다.', '문제를 읽을 때 조회 범위 컬럼이 무엇인지 먼저 체크해야 덜 헤맨다.', TIMESTAMP '2026-03-03 08:40:00', ARRAY['00001-00001', '문제풀이', '조인', '초보자']),
(4, 'community-seed-0004', 4, '00001-00001 처음 제출했다가 중복 합계 나온 원인', '주문별 합계를 먼저 만들지 않고 바로 고객별로 묶다가 금액이 예상보다 커졌다.', '테스트 데이터를 손으로 계산해 보니 한 주문의 여러 상품이 그대로 반영되는 구조였다.', '샘플 데이터를 먼저 손으로 계산하면 쿼리 실수를 빨리 찾는다.', TIMESTAMP '2026-03-03 11:15:00', ARRAY['00001-00001', '문제풀이', '합계', '초보자']),
(5, 'community-seed-0005', 5, '00001-00001 WHERE 절 기간 조건 체크리스트', '이 문제는 ordered_at의 상한과 하한을 정확히 맞추는 것부터 시작했다.', 'BETWEEN 대신 이상 미만 조건으로 적어 두니 4월 데이터가 섞이는 걸 막을 수 있었다.', '시간 범위 문제는 이상 미만으로 쓰는 습관이 디버깅에 유리했다.', TIMESTAMP '2026-03-04 09:05:00', ARRAY['00001-00001', '문제풀이', 'WHERE', '초보자']),
(6, 'community-seed-0006', 6, 'LEFT JOIN 하면 행이 늘어나는 이유가 궁금합니다', '1:N 관계에서 상세 테이블을 그대로 붙이면 기준 행 수가 늘어나는 상황이 자주 나온다.', '주문, 주문상품처럼 하위 행이 여러 개일 때 어떤 컬럼을 세는지에 따라 결과가 달라진다.', 'JOIN 후 결과 행을 먼저 확인하면 대부분의 중복 문제는 설명된다.', TIMESTAMP '2026-03-04 14:30:00', ARRAY['조인', '질문', '집계']),
(7, 'community-seed-0007', 7, 'ORDER BY를 꼭 마지막에만 써야 하나요?', '서브쿼리 안쪽 정렬과 최종 결과 정렬은 역할이 다르다.', '최종 출력 순서가 필요할 때만 바깥 SELECT에 ORDER BY를 두는 편이 읽기 쉽다.', '정렬은 필요한 위치에서만 쓰는 게 실행 계획도 덜 복잡해진다.', TIMESTAMP '2026-03-05 09:20:00', ARRAY['정렬', '질문', 'SQL기초']),
(8, 'community-seed-0008', 8, '인덱스가 있는데도 느린 이유를 잘 모르겠습니다', '인덱스가 있어도 조건 선택도가 낮거나 정렬과 맞지 않으면 체감 성능이 잘 안 나올 수 있다.', '실행 계획에서 실제로 인덱스를 타는지와 필터링 비율을 같이 봐야 한다.', '인덱스 존재 여부보다 어떤 조건과 결합되는지가 먼저였다.', TIMESTAMP '2026-03-05 13:55:00', ARRAY['인덱스', '질문', '튜닝']),
(9, 'community-seed-0009', 9, 'GROUP BY와 DISTINCT 차이를 쉽게 설명해 주세요', '둘 다 중복 제거처럼 보이지만 집계가 들어가면 목적이 완전히 달라진다.', 'GROUP BY는 묶은 뒤 계산하고 DISTINCT는 선택된 열 조합만 중복 제거한다.', '두 문법이 비슷해 보여도 집계가 시작되면 읽는 방식부터 달라진다.', TIMESTAMP '2026-03-06 10:40:00', ARRAY['GROUP BY', 'DISTINCT', '질문']),
(10, 'community-seed-0010', 10, 'MySQL에서 LIMIT가 먼저 눈에 들어옵니다', 'LIMIT에 익숙한 입장에서는 상위 N건 조회 의도가 바로 보였다.', '정렬 후 상위 N건을 가져오는 흐름을 읽기 쉽게 표현할 수 있는 점이 좋았다.', 'DBMS마다 익숙한 문법은 달라도 결국 의도를 분명히 적는 게 우선이다.', TIMESTAMP '2026-03-06 16:05:00', ARRAY['MySQL', '질문', '페이징']),
(11, 'community-seed-0011', 11, '00001-00001 서브쿼리 없이 푼 버전 공유', '이번 문제는 필요한 조인만 남기고 바로 고객 단위로 그룹핑해도 충분히 풀렸다.', '핵심은 COUNT DISTINCT와 SUM 대상이 다른 이유를 분리해서 이해하는 것이었다.', '문제를 푸는 데 항상 서브쿼리가 필요한 것은 아니었다.', TIMESTAMP '2026-03-07 09:00:00', ARRAY['00001-00001', '문제풀이', '중급자', 'GROUP BY']),
(12, 'community-seed-0012', 12, '00001-00001 GROUP BY 최소화한 풀이', '고객별로 필요한 컬럼만 남겨 GROUP BY 대상을 최대한 줄였다.', 'title처럼 필요 없는 컬럼을 끌어오지 않으니 쿼리도 짧고 검증도 쉬웠다.', 'SELECT에 올리는 컬럼이 늘면 GROUP BY도 함께 무거워진다.', TIMESTAMP '2026-03-07 13:15:00', ARRAY['00001-00001', '문제풀이', '중급자', '집계']),
(13, 'community-seed-0013', 13, '00001-00001 실행 계획 보고 바꾼 포인트', '실행 계획에서 orders 범위 필터가 먼저 걸리는지와 조인 후 행 수를 집중해서 봤다.', '의도는 단순했지만 계획을 보고 나니 어떤 조건이 먼저 줄여 주는지 더 명확했다.', '실행 계획은 정답 확인이 아니라 행 수 흐름 확인용으로 보는 습관이 좋았다.', TIMESTAMP '2026-03-08 08:50:00', ARRAY['00001-00001', '문제풀이', '중급자', '실행계획']),
(14, 'community-seed-0014', 14, '00001-00001 SUM 대상 컬럼 다시 본 후기', '총 주문 금액은 할인 금액이 아니라 quantity와 unit_price를 곱한 값을 합하는 문제였다.', '문제 문장을 그대로 컬럼식으로 옮겨 적으니 헷갈리던 부분이 정리됐다.', '집계 문제는 자연어 조건을 식으로 번역하는 과정이 중요했다.', TIMESTAMP '2026-03-08 11:35:00', ARRAY['00001-00001', '문제풀이', '중급자', 'SUM']),
(15, 'community-seed-0015', 15, '00001-00001 DISTINCT 위치에 따라 결과 달라진 이유', 'DISTINCT를 SELECT 전체에 두는 것과 COUNT 안에 두는 것은 결과가 완전히 달랐다.', '주문 수만 DISTINCT 대상이어야 하는데 전체 행을 DISTINCT로 만들면 금액 계산도 영향을 받는다.', '중복 제거의 위치를 잘못 두면 정답과 성능을 같이 잃는다.', TIMESTAMP '2026-03-08 17:25:00', ARRAY['00001-00001', '문제풀이', '중급자', 'DISTINCT']),
(16, 'community-seed-0016', 16, 'PostgreSQL에서 CTE가 항상 느린 건 아닌 것 같습니다', '최근 버전에서는 CTE가 무조건 최적화 장벽이 아니어서 예전 감각만 믿으면 오해하기 쉽다.', '의도를 분리해서 읽기 좋게 만들고 실제 계획을 확인한 뒤 판단하는 편이 안전했다.', '문법에 대한 인상보다 실제 플랜을 먼저 보는 습관이 필요했다.', TIMESTAMP '2026-03-09 09:10:00', ARRAY['PostgreSQL', 'CTE', '튜닝']),
(17, 'community-seed-0017', 17, '윈도우 함수로 순위 매길 때 자주 쓰는 패턴 정리', 'ROW_NUMBER와 RANK, DENSE_RANK를 문제 성격에 맞게 쓰는 기준을 간단히 정리했다.', '동점 처리 기준과 ORDER BY가 바뀌면 결과 해석도 함께 달라진다.', '윈도우 함수는 정렬 기준을 먼저 말로 설명해 보면 더 쉽게 고를 수 있다.', TIMESTAMP '2026-03-09 14:45:00', ARRAY['윈도우함수', '정리', '중급자']),
(18, 'community-seed-0018', 18, '트랜잭션 격리 수준 바꾸면 어떤 일이 생기나요', '같은 쿼리여도 격리 수준이 달라지면 읽을 수 있는 데이터 범위와 잠금 영향이 달라진다.', '단순히 성능 문제가 아니라 어떤 현상을 허용할지 결정하는 설정이라고 느꼈다.', '격리 수준은 성능보다 일관성 요구사항부터 보고 고르는 편이 낫다.', TIMESTAMP '2026-03-10 08:35:00', ARRAY['트랜잭션', '질문', '중급자']),
(19, 'community-seed-0019', 19, '실행 계획에서 Hash Join이 나왔을 때 체크한 것들', '무조건 Nested Loop가 좋은 게 아니라 입력 행 수와 메모리 상황에 따라 Hash Join이 더 낫기도 했다.', '조인 방식 이름보다 실제 입력 건수와 필터 순서를 먼저 확인했다.', '조인 방식은 결과가 아니라 상황의 표현이라고 보는 편이 이해가 쉬웠다.', TIMESTAMP '2026-03-10 12:50:00', ARRAY['실행계획', '조인', '중급자']),
(20, 'community-seed-0020', 20, '복합 인덱스를 만들 때 컬럼 순서를 어떻게 정하나요', '같은 컬럼 집합이어도 앞쪽 컬럼 순서에 따라 실제 도움이 되는 쿼리가 크게 달라졌다.', '자주 쓰는 동등 조건과 정렬 컬럼을 함께 보는 방식이 가장 실무적이었다.', '인덱스 순서는 쿼리 패턴의 빈도를 먼저 적어 보면 결정이 쉬워졌다.', TIMESTAMP '2026-03-10 17:20:00', ARRAY['인덱스', '질문', '중급자']),
(21, 'community-seed-0021', 21, '00001-00001 정답은 맞는데 느렸던 쿼리 개선', '같은 결과를 내더라도 불필요한 정렬과 넓은 SELECT 범위 때문에 실제 실행은 더 느렸다.', '필요한 컬럼만 남기고 범위를 먼저 줄이니 계획이 단순해졌다.', '정답과 성능은 별도 검증 항목이라는 점이 이 문제에서 분명했다.', TIMESTAMP '2026-03-11 09:25:00', ARRAY['00001-00001', '문제풀이', '상급자', '튜닝']),
(22, 'community-seed-0022', 22, '00001-00001 조인 순서보다 중요한 집계 범위', '고객별 집계를 하기 전에 어떤 범위를 먼저 줄일지 정하는 쪽이 체감 성능에 더 큰 영향을 줬다.', '조인 순서를 고민하기 전에 3월 주문만 정확히 자르는 게 선행되어야 했다.', '행 수를 먼저 줄이는 쿼리가 결국 읽기도 좋고 빠르기도 했다.', TIMESTAMP '2026-03-11 13:40:00', ARRAY['00001-00001', '문제풀이', '상급자', '집계']),
(23, 'community-seed-0023', 23, '00001-00001 불필요한 정렬 없이 통과한 쿼리', '최종 출력 정렬만 남기고 중간 단계 정렬을 모두 제거했더니 계획이 훨씬 단순해졌다.', '집계 전에 ORDER BY를 넣는 습관이 있었는데 이번에 확실히 정리됐다.', '정렬은 필요한 마지막 순간에만 두는 게 낫다.', TIMESTAMP '2026-03-12 08:55:00', ARRAY['00001-00001', '문제풀이', '상급자', '정렬']),
(24, 'community-seed-0024', 24, '00001-00001 실제 튜닝할 때 본 실행 계획 메모', '주문 범위 필터 이후 조인되는 행 수와 집계 단계의 비용을 메모하면서 비교했다.', '결과만 맞는 쿼리와 실행 흐름이 깔끔한 쿼리는 체감상 분명히 달랐다.', '실행 계획은 숫자보다 흐름을 읽는 도구로 보는 편이 유지보수에 좋았다.', TIMESTAMP '2026-03-12 11:30:00', ARRAY['00001-00001', '문제풀이', '상급자', '실행계획']),
(25, 'community-seed-0025', 25, '00001-00001 같은 결과를 더 단순하게 만든 버전', '중간 계산을 줄이고 바로 필요한 집계만 남기니 쿼리가 훨씬 짧아졌다.', '짧은 쿼리가 무조건 좋은 건 아니지만, 이번 문제는 단순한 형태가 검증에도 유리했다.', '단순한 쿼리가 결국 테스트와 비교에도 강했다.', TIMESTAMP '2026-03-12 17:10:00', ARRAY['00001-00001', '문제풀이', '상급자', '리팩터링']),
(26, 'community-seed-0026', 26, '대량 UPDATE 전에 확인하는 잠금과 인덱스 체크리스트', '변경 대상 범위와 잠금 지속 시간을 먼저 보지 않으면 배포 시간에 사고가 나기 쉽다.', '대량 변경 전에는 조건 컬럼 인덱스와 배치 크기를 같이 확인하는 편이 안정적이었다.', '쓰기 작업은 빠른 쿼리보다 안전한 절차가 먼저였다.', TIMESTAMP '2026-03-13 09:15:00', ARRAY['UPDATE', '잠금', '상급자']),
(27, 'community-seed-0027', 27, '정규화와 조회 성능 사이에서 기준을 어떻게 잡는지', '정규화가 항상 옳거나 비정규화가 항상 빠른 것은 아니라는 점을 사례 중심으로 적었다.', '쓰기 빈도와 조회 패턴이 분명하지 않으면 구조를 먼저 단순하게 가져가는 편이 낫다.', '구조 선택은 성능 숫자보다 변경 비용까지 같이 봐야 한다.', TIMESTAMP '2026-03-13 13:05:00', ARRAY['모델링', '성능', '상급자']),
(28, 'community-seed-0028', 28, 'MySQL 힌트는 언제부터 유지보수 비용이 커지는지', '힌트가 즉시 효과를 줄 때도 있지만 쿼리와 통계가 바뀌면 유지 비용이 빠르게 커진다.', '문제 해결 후에도 왜 힌트가 필요했는지 기록하지 않으면 나중에 더 위험했다.', '힌트는 최후 수단에 가깝게 쓰는 편이 결국 안정적이었다.', TIMESTAMP '2026-03-14 08:45:00', ARRAY['MySQL', '힌트', '상급자']),
(29, 'community-seed-0029', 29, '실무에서 커버링 인덱스 비슷하게 가져가는 방식', '모든 상황에서 완전한 커버링을 노리기보다 자주 보는 컬럼 묶음을 우선 잡는 편이 효율적이었다.', '읽기 전용 화면과 쓰기 많은 화면을 분리해서 보면 인덱스 전략이 더 선명해진다.', '인덱스는 많을수록 좋은 자산이 아니라 유지해야 하는 비용이었다.', TIMESTAMP '2026-03-14 12:30:00', ARRAY['인덱스', '튜닝', '상급자']),
(30, 'community-seed-0030', 30, '댓글 많은 게시글 정렬을 DB에서 다룰 때 주의한 점', '집계 테이블 없이 댓글 수 정렬을 자주 쓰면 본문 조회보다 목록 비용이 더 커질 수 있다.', '정렬 기준 컬럼을 미리 관리하거나 검색 인덱스로 넘기는 기준을 미리 정해 두는 게 좋았다.', '목록 요구사항이 바뀌면 저장 구조도 함께 바뀔 수 있다는 걸 자주 본다.', TIMESTAMP '2026-03-14 17:40:00', ARRAY['정렬', '댓글', '상급자']);

DELETE FROM quertimizer.community_comment_like
WHERE comment_id IN (
    SELECT comment_id
    FROM quertimizer.community_comment
    WHERE post_id IN (SELECT post_id FROM tmp_community_post_seed)
);

DELETE FROM quertimizer.community_comment
WHERE post_id IN (SELECT post_id FROM tmp_community_post_seed);

DELETE FROM quertimizer.community_post_like
WHERE post_id IN (SELECT post_id FROM tmp_community_post_seed);

DELETE FROM quertimizer.community_post_tag
WHERE post_id IN (SELECT post_id FROM tmp_community_post_seed);

DELETE FROM quertimizer.community_post
WHERE post_id IN (SELECT post_id FROM tmp_community_post_seed);

INSERT INTO quertimizer.community_post (
    post_id,
    user_id,
    title,
    content_html,
    content_text,
    view_count,
    like_count,
    comment_count,
    created_at,
    updated_at
)
SELECT
    posts.post_id,
    users.user_id,
    posts.title,
    '<h2>' || posts.title || '</h2>'
        || '<p><b>핵심</b> ' || posts.summary || '</p>'
        || '<p>' || posts.detail || '</p>'
        || '<blockquote>' || posts.quote_text || '</blockquote>',
    posts.title || ' ' || posts.summary || ' ' || posts.detail || ' ' || posts.quote_text,
    40
        + (posts.post_seq * 13)
        + CASE
            WHEN posts.user_seq <= 10 THEN 0
            WHEN posts.user_seq <= 20 THEN 35
            ELSE 70
        END,
    CASE
        WHEN posts.post_seq % 3 = 1 THEN 2
        WHEN posts.post_seq % 3 = 2 THEN 3
        ELSE 4
    END,
    CASE
        WHEN posts.post_seq % 4 = 0 THEN 2
        ELSE 1
    END,
    posts.created_at,
    CASE
        WHEN posts.post_seq % 5 = 0 THEN posts.created_at + INTERVAL '6 hour'
        ELSE NULL
    END
FROM tmp_community_post_seed posts
JOIN tmp_community_user_seed users
    ON users.user_seq = posts.user_seq;

INSERT INTO quertimizer.community_post_tag (
    post_id,
    tag,
    tag_order
)
SELECT
    posts.post_id,
    tag,
    tag_order - 1
FROM tmp_community_post_seed posts
CROSS JOIN LATERAL unnest(posts.tags) WITH ORDINALITY AS tag_rows(tag, tag_order);

INSERT INTO quertimizer.community_post_like (
    post_id,
    user_id,
    created_at
)
SELECT
    posts.post_id,
    liker.user_id,
    posts.created_at + (like_order * INTERVAL '25 minute')
FROM tmp_community_post_seed posts
CROSS JOIN LATERAL generate_series(
    1,
    CASE
        WHEN posts.post_seq % 3 = 1 THEN 2
        WHEN posts.post_seq % 3 = 2 THEN 3
        ELSE 4
    END
) AS like_order
JOIN tmp_community_user_seed liker
    ON liker.user_seq = (((posts.user_seq + like_order) - 1) % 30) + 1;

INSERT INTO quertimizer.community_comment (
    comment_id,
    post_id,
    user_id,
    parent_comment_id,
    content,
    like_count,
    created_at
)
SELECT
    900000 + (posts.post_seq * 10) + 1,
    posts.post_id,
    commenter.user_id,
    NULL,
    CASE
        WHEN array_position(posts.tags, '00001-00001') IS NOT NULL THEN '조건을 먼저 정리한 방식이 이해에 도움이 됐습니다. 특히 주문 수를 DISTINCT로 본 부분이 깔끔했습니다.'
        ELSE '실무에서 비슷하게 고민한 적이 있어서 정리 방식이 꽤 도움이 됐습니다. 추가 예시가 있으면 더 보고 싶습니다.'
    END,
    1,
    posts.created_at + INTERVAL '3 hour'
FROM tmp_community_post_seed posts
JOIN tmp_community_user_seed commenter
    ON commenter.user_seq = (((posts.user_seq + 3) - 1) % 30) + 1;

INSERT INTO quertimizer.community_comment (
    comment_id,
    post_id,
    user_id,
    parent_comment_id,
    content,
    like_count,
    created_at
)
SELECT
    900000 + (posts.post_seq * 10) + 2,
    posts.post_id,
    replier.user_id,
    900000 + (posts.post_seq * 10) + 1,
    CASE
        WHEN array_position(posts.tags, '00001-00001') IS NOT NULL THEN '저도 비슷하게 풀었는데 집계 기준을 따로 적어 두니 훨씬 덜 헷갈렸습니다.'
        ELSE '저도 같은 포인트를 메모해 두고 있었는데, 질문 의도가 더 선명하게 정리된 것 같습니다.'
    END,
    1,
    posts.created_at + INTERVAL '5 hour'
FROM tmp_community_post_seed posts
JOIN tmp_community_user_seed replier
    ON replier.user_seq = (((posts.user_seq + 4) - 1) % 30) + 1
WHERE posts.post_seq % 4 = 0;

INSERT INTO quertimizer.community_comment_like (
    comment_id,
    user_id,
    created_at
)
SELECT
    comments.comment_id,
    liker.user_id,
    comments.created_at + (like_order * INTERVAL '12 minute')
FROM quertimizer.community_comment comments
JOIN tmp_community_post_seed posts
    ON posts.post_id = comments.post_id
CROSS JOIN LATERAL generate_series(1, comments.like_count) AS like_order
JOIN tmp_community_user_seed liker
    ON liker.user_seq = (((posts.user_seq + like_order + 6) - 1) % 30) + 1
WHERE comments.comment_id BETWEEN 900011 AND 900302;

SELECT setval(
    pg_get_serial_sequence('quertimizer.community_comment', 'comment_id'),
    COALESCE((SELECT MAX(comment_id) FROM quertimizer.community_comment), 1),
    true
);

DROP TABLE tmp_community_post_seed;
DROP TABLE tmp_community_user_seed;
