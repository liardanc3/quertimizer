import type { CommunityPostCategory, CommunityPostSummary, CommunityTagDefinition } from '../types/domain';

export const mockCommunityTagLibrary: CommunityTagDefinition[] = [
  {
    id: 'problem-101',
    label: '101번',
    kind: 'problem',
    aliases: ['101', 'p101', '문제101', 'problem101'],
    usageCount: 18,
    description: '문제 101 관련 풀이와 질문을 묶는 태그',
  },
  {
    id: 'problem-205',
    label: '205번',
    kind: 'problem',
    aliases: ['205', 'p205', '문제205', 'problem205'],
    usageCount: 11,
    description: '문제 205 관련 최적화 포인트를 모아보는 태그',
  },
  {
    id: 'problem-310',
    label: '310번',
    kind: 'problem',
    aliases: ['310', 'p310', '문제310', 'problem310'],
    usageCount: 7,
    description: '문제 310 관련 질문과 풀이 공유용 태그',
  },
  {
    id: 'left-join',
    label: 'LEFT JOIN',
    kind: 'tech',
    aliases: ['left_join', 'left join', 'leftjoin'],
    usageCount: 26,
    description: '외부 조인과 null 보존 여부를 다루는 태그',
  },
  {
    id: 'group-by',
    label: 'GROUP BY',
    kind: 'tech',
    aliases: ['group_by', 'group by', 'aggregation'],
    usageCount: 21,
    description: '집계와 중복 제거 패턴을 찾을 때 쓰는 태그',
  },
  {
    id: 'window-function',
    label: 'WINDOW FUNCTION',
    kind: 'tech',
    aliases: ['window_function', 'window function', 'analytic function'],
    usageCount: 17,
    description: '윈도우 함수, 순위 함수, 누적 합을 정리할 때 쓰는 태그',
  },
  {
    id: 'index',
    label: 'INDEX',
    kind: 'tech',
    aliases: ['index scan', '인덱스', 'btree'],
    usageCount: 23,
    description: '인덱스 설계와 액세스 패턴 논의를 모으는 태그',
  },
  {
    id: 'execution-plan',
    label: '실행계획',
    kind: 'topic',
    aliases: ['execution plan', 'explain', 'plan'],
    usageCount: 22,
    description: '실행계획 읽기, Explain 분석, 플랜 비교용 태그',
  },
  {
    id: 'cte',
    label: 'CTE',
    kind: 'tech',
    aliases: ['with clause', 'common table expression', 'with'],
    usageCount: 16,
    description: 'WITH 절, 단계별 쿼리 분리, materialize 이슈용 태그',
  },
  {
    id: 'postgresql',
    label: 'PostgreSQL',
    kind: 'topic',
    aliases: ['postgres', 'postgresql', 'pg'],
    usageCount: 19,
    description: 'PostgreSQL 전용 힌트와 실행계획 차이를 다루는 태그',
  },
  {
    id: 'oracle',
    label: 'Oracle',
    kind: 'topic',
    aliases: ['oracle db', 'ora', 'oracle'],
    usageCount: 14,
    description: 'Oracle 최적화와 문법 차이를 위한 태그',
  },
  {
    id: 'hash-join',
    label: 'HASH JOIN',
    kind: 'tech',
    aliases: ['hash_join', 'hash join'],
    usageCount: 9,
    description: '조인 방식 비교와 메모리 사용량을 다루는 태그',
  },
  {
    id: 'tuning',
    label: '튜닝',
    kind: 'topic',
    aliases: ['tuning', 'performance', 'optimize'],
    usageCount: 20,
    description: '실행시간, scan rows, 병목 개선 논의를 위한 태그',
  },
  {
    id: 'tag-policy',
    label: '태그 운영',
    kind: 'topic',
    aliases: ['tag_policy', '태그정책', 'tag guide'],
    usageCount: 6,
    description: '커뮤니티 태그 규칙과 표기 기준을 안내하는 태그',
  },
];

const postTemplates: Array<{
  title: string;
  excerpt: string;
  content: string;
  tags: string[];
  category: CommunityPostCategory;
  baseViews: number;
  baseLikes: number;
  baseComments: number;
  isPinned?: boolean;
  isResolved?: boolean;
}> = [
  {
    title: '101번에서 LEFT JOIN 결과가 두 배로 불어나는 이유 정리',
    excerpt: '집계 전에 중복 행이 늘어나는 지점을 먼저 체크하면 문제 101 풀이가 훨씬 안정적이었습니다.',
    content:
      '문제 101을 풀다가 LEFT JOIN 뒤에 바로 COUNT(*)를 사용하면 주문 행이 예상보다 크게 늘어났습니다. 먼저 오른쪽 테이블을 사전 집계한 뒤 GROUP BY를 적용하는 식으로 정리했습니다.',
    tags: ['101번', 'LEFT JOIN', 'GROUP BY', 'PostgreSQL'],
    category: 'tip',
    baseViews: 482,
    baseLikes: 73,
    baseComments: 18,
  },
  {
    title: 'LEFT JOIN에서 ON 절과 WHERE 절 필터 위치 차이가 아직 헷갈립니다',
    excerpt: 'NULL 보존 차이를 이해했는데 실전 문제에서는 어디를 먼저 의심해야 할지 고민입니다.',
    content:
      'LEFT JOIN 뒤 WHERE 절에서 right table 조건을 걸면 inner join처럼 보일 때가 많았습니다. 조인 결과와 실행계획을 어떤 순서로 확인해야 하는지 정리했습니다.',
    tags: ['LEFT JOIN', '실행계획', '205번'],
    category: 'question',
    baseViews: 356,
    baseLikes: 28,
    baseComments: 21,
    isResolved: true,
  },
  {
    title: 'WINDOW FUNCTION 체감 속도를 올린 CTE 분리 패턴 공유',
    excerpt: 'ROW_NUMBER와 누적 합을 한 번에 처리하다 느려진 쿼리를 단계별로 나눠본 사례입니다.',
    content:
      'WINDOW FUNCTION을 한 쿼리에 몰아 넣었더니 정렬 비용이 커졌습니다. CTE로 범위를 먼저 줄이고 마지막 단계에서만 적용하니 성능이 한결 안정됐습니다.',
    tags: ['WINDOW FUNCTION', 'CTE', 'PostgreSQL', '튜닝'],
    category: 'tip',
    baseViews: 291,
    baseLikes: 64,
    baseComments: 12,
  },
  {
    title: 'Oracle 실행계획 읽을 때 어떤 칼럼부터 보시나요?',
    excerpt: 'Rows, Cost, Predicate Information 중 어디부터 보면 좋은지 기준을 모아봤습니다.',
    content:
      'Oracle 쿼리를 리뷰할 때 Rows와 Cost만 먼저 보는 편이었는데, Predicate Information과 조인 순서를 먼저 보는 팀도 있어 비교 관점을 정리했습니다.',
    tags: ['Oracle', '실행계획', '튜닝'],
    category: 'discussion',
    baseViews: 244,
    baseLikes: 31,
    baseComments: 27,
  },
  {
    title: '205번에서 HASH JOIN이 Nested Loop보다 느린 이유가 뭘까요?',
    excerpt: '카디널리티 추정이 흔들리는 것 같긴 한데 어디부터 손봐야 할지 질문드립니다.',
    content:
      '문제 205에서 HASH JOIN으로 바꾸면 좋아질 줄 알았는데 메모리 사용량이 커지면서 전체 시간이 더 늘었습니다. 빌드 입력 쪽과 INDEX 활용을 다시 확인했습니다.',
    tags: ['205번', 'HASH JOIN', 'INDEX', '실행계획'],
    category: 'question',
    baseViews: 327,
    baseLikes: 19,
    baseComments: 16,
    isResolved: false,
  },
  {
    title: '커뮤니티 태그 작성 기준 안내',
    excerpt: '문제 번호 태그와 기술 태그를 함께 쓰고 기존 표기를 우선 선택하는 규칙입니다.',
    content:
      '커뮤니티에서는 문제 번호 태그와 기술 태그를 함께 사용할 수 있습니다. LEFT JOIN과 left_join처럼 의미가 같은 태그는 하나의 표기로 정리하는 방향으로 안내하고 있습니다.',
    tags: ['태그 운영', 'LEFT JOIN', '101번'],
    category: 'notice',
    baseViews: 612,
    baseLikes: 88,
    baseComments: 14,
    isPinned: true,
  },
  {
    title: 'INDEX만 추가하면 해결될 줄 알았는데 아닌 케이스',
    excerpt: '선택도가 낮은 컬럼에 INDEX를 추가해도 큰 차이가 없었던 사례입니다.',
    content:
      '조건절에 자주 쓰인다고 무조건 INDEX를 추가했는데 정작 범위가 너무 넓어서 FULL SCAN과 큰 차이가 나지 않았습니다. 실행계획과 실제 rows를 같이 비교했습니다.',
    tags: ['INDEX', '실행계획', '튜닝'],
    category: 'tip',
    baseViews: 418,
    baseLikes: 58,
    baseComments: 9,
  },
  {
    title: 'PostgreSQL에서 CTE materialize를 언제 의심하시나요?',
    excerpt: 'WITH 절이 가독성은 좋은데 실제 플랜에서 병목이 될 때가 있습니다.',
    content:
      'PostgreSQL에서는 버전에 따라 CTE가 inline 되거나 materialize 되는데, 어떤 시점에 서브쿼리로 바꿔야 하는지 기준을 모았습니다.',
    tags: ['CTE', 'PostgreSQL', '실행계획'],
    category: 'discussion',
    baseViews: 233,
    baseLikes: 34,
    baseComments: 20,
  },
  {
    title: '310번 Oracle 풀이에서 GROUP BY 전에 서브쿼리 분리해야 할까요?',
    excerpt: '조인 후 집계보다 집계 후 조인이 낫다는 건 아는데 Oracle에서 항상 같은 결론인지 궁금합니다.',
    content:
      '문제 310을 Oracle로 풀고 있는데 GROUP BY 전에 작은 단위로 먼저 집계하는 게 낫다는 조언을 들었습니다. HASH JOIN과 GROUP BY 비용이 함께 올라가는 구간을 정리했습니다.',
    tags: ['310번', 'Oracle', 'GROUP BY', 'HASH JOIN'],
    category: 'question',
    baseViews: 279,
    baseLikes: 24,
    baseComments: 13,
    isResolved: true,
  },
  {
    title: '실행계획 캡처 없이도 질문을 잘 올리는 템플릿',
    excerpt: '질문글을 올릴 때 어떤 정보가 있어야 답변이 빨라지는지 체크리스트를 정리했습니다.',
    content:
      '실행계획 스크린샷이 없어도 테이블 크기, 사용한 JOIN 종류, 예상 결과와 실제 결과, 시도한 INDEX 변경점만 적어도 답변 속도가 빨라졌습니다.',
    tags: ['실행계획', '튜닝', '태그 운영'],
    category: 'tip',
    baseViews: 364,
    baseLikes: 71,
    baseComments: 11,
  },
];

const authorHandles = [
  'minseo_db',
  'junho_plan',
  'seoyoon_sql',
  'dohyun_ora',
  'haneul_exec',
  'speedql',
  'yujin_idx',
  'jiwoo_pg',
  'sehun_sql',
  'sua_notes',
  'leftjoin_lab',
  'hashjoiner',
  'oracle_tune',
  'groupby_dev',
  'cte_runner',
  'plan_reader',
  'rdbms_mate',
  'sqlwalker',
  'indexroom',
  'window_fox',
];

const SEOUL_OFFSET_MS = 9 * 60 * 60 * 1000;
const BASE_TIMESTAMP = Date.UTC(2026, 2, 30, 13, 20, 0);

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function formatSeoulIso(timestamp: number) {
  const seoulDate = new Date(timestamp + SEOUL_OFFSET_MS);
  const year = seoulDate.getUTCFullYear();
  const month = pad(seoulDate.getUTCMonth() + 1);
  const day = pad(seoulDate.getUTCDate());
  const hours = pad(seoulDate.getUTCHours());
  const minutes = pad(seoulDate.getUTCMinutes());

  return `${year}-${month}-${day}T${hours}:${minutes}:00+09:00`;
}

export const mockCommunityPosts: CommunityPostSummary[] = Array.from({ length: 200 }, (_, index) => {
  const template = postTemplates[index % postTemplates.length];
  const cycle = Math.floor(index / postTemplates.length);
  const authorHandle = authorHandles[(index * 7) % authorHandles.length];
  const createdAt = formatSeoulIso(BASE_TIMESTAMP - index * 1000 * 60 * 95);
  const sequenceLabel = cycle > 0 ? ` · 사례 ${cycle + 1}` : '';

  return {
    id: `community-${String(index + 1).padStart(3, '0')}`,
    title: `${template.title}${sequenceLabel}`,
    authorHandle,
    excerpt: template.excerpt,
    content: `${template.content} 작성자 아이디는 ${authorHandle} 기준으로 남겨 두었습니다.`,
    tags: template.tags,
    category: template.category,
    createdAt,
    views: template.baseViews + cycle * 17 + ((index * 13) % 91),
    likes: template.baseLikes + cycle * 3 + ((index * 5) % 17),
    comments: template.baseComments + (index % 11),
    isPinned: Boolean(template.isPinned && cycle === 0),
    isResolved: template.isResolved,
  };
});
