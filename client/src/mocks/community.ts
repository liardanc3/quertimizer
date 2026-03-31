import type {
  CommunityComment,
  CommunityPostCategory,
  CommunityPostSummary,
  CommunityTagDefinition,
} from '../types/domain';
import { mockCurrentHandle } from './profile';

export const mockCommunityTagLibrary: CommunityTagDefinition[] = [
  { id: 'problem-101', label: '101번', kind: 'problem', aliases: ['101', 'p101', 'problem101', 'vip-score'], usageCount: 18, description: 'VIP 고객 세그먼트 문제 관련 토론을 묶는 태그입니다.' },
  { id: 'problem-214', label: '214번', kind: 'problem', aliases: ['214', 'p214', 'problem214', 'monthly-top3'], usageCount: 11, description: '월별 상위 상품 매출 문제 풀이를 모아보는 태그입니다.' },
  { id: 'problem-305', label: '305번', kind: 'problem', aliases: ['305', 'p305', 'problem305', 'sla-delay'], usageCount: 9, description: '지점별 SLA 지연 비율 문제 토론용 태그입니다.' },
  { id: 'left-join', label: 'LEFT JOIN', kind: 'tech', aliases: ['left_join', 'leftjoin'], usageCount: 25, description: 'LEFT JOIN 의미와 null 보존 이슈를 다루는 태그입니다.' },
  { id: 'group-by', label: 'GROUP BY', kind: 'tech', aliases: ['group_by', 'aggregate', 'aggregation'], usageCount: 23, description: '집계, 정렬, 중복 제거 흐름을 정리할 때 쓰는 태그입니다.' },
  { id: 'window-function', label: 'WINDOW FUNCTION', kind: 'tech', aliases: ['window', 'analytic-function'], usageCount: 20, description: '윈도우 함수, 순위 함수, 프레임 설계를 다루는 태그입니다.' },
  { id: 'index', label: 'INDEX', kind: 'tech', aliases: ['index-scan', 'btree', 'covering-index'], usageCount: 28, description: '인덱스 설계와 액세스 경로 선택을 다루는 태그입니다.' },
  { id: 'execution-plan', label: '실행계획', kind: 'topic', aliases: ['plan', 'explain', 'explain-analyze'], usageCount: 26, description: '플랜 해석, 연산자 선택, 병목 구간 분석에 쓰는 태그입니다.' },
  { id: 'cte', label: 'CTE', kind: 'tech', aliases: ['with', 'common-table-expression'], usageCount: 16, description: 'CTE 인라인, materialize, 재사용 패턴을 다루는 태그입니다.' },
  { id: 'postgresql', label: 'PostgreSQL', kind: 'topic', aliases: ['postgres', 'pg'], usageCount: 21, description: 'PostgreSQL 전용 동작과 튜닝 차이를 정리하는 태그입니다.' },
  { id: 'oracle', label: 'Oracle', kind: 'topic', aliases: ['ora', 'oracle-db'], usageCount: 14, description: 'Oracle 전용 실행계획과 문법 차이를 다루는 태그입니다.' },
  { id: 'tuning', label: '튜닝', kind: 'topic', aliases: ['optimize', 'performance'], usageCount: 24, description: 'SQL 챌린지의 성능 개선 패턴을 모아보는 태그입니다.' },
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
    title: '101번에서 LEFT JOIN 조건 위치를 바꾸니 결과 건수가 달라진 이유 정리',
    excerpt: 'WHERE 절 조건을 ON 절로 옮기자 결과 건수와 점수가 함께 달라졌던 사례입니다.',
    content:
      '상태 조건을 WHERE 절에서 ON 절로 옮기니 null 쪽 행이 더 이상 사라지지 않았습니다. 결과는 맞아졌지만 왜 채점 결과가 달라지는지 설명하는 흐름을 정리해봤습니다.',
    tags: ['101번', 'LEFT JOIN', 'GROUP BY', 'PostgreSQL'],
    category: 'tip',
    baseViews: 482,
    baseLikes: 73,
    baseComments: 18,
  },
  {
    title: '214번에서 팩트 테이블 전체 정렬 없이 월별 상위 3개를 구한 방법',
    excerpt: '동점 처리까지 유지하면서 월별 랭킹 쿼리를 가볍게 만든 과정을 공유합니다.',
    content:
      '단순한 풀이로는 랭킹 전에 너무 많은 데이터를 정렬하게 됩니다. 먼저 좁은 범위로 집계한 뒤 윈도우 함수를 적용해 핵심 병목을 줄였습니다.',
    tags: ['214번', 'WINDOW FUNCTION', '실행계획', '튜닝'],
    category: 'question',
    baseViews: 356,
    baseLikes: 28,
    baseComments: 21,
    isResolved: true,
  },
  {
    title: 'CTE materialize를 언제 의심해야 하는지 정리',
    excerpt: '가독성은 좋아지지만 성능이 나빠지는 CTE 패턴을 짧게 정리했습니다.',
    content:
      '어떤 플랜에서는 CTE가 인라인되어 무난하지만, 어떤 경우에는 블로킹 단계가 되면서 정렬이나 spill 부담이 커집니다. 최근 제출 사례를 모아 비교했습니다.',
    tags: ['CTE', 'PostgreSQL', '실행계획', '튜닝'],
    category: 'tip',
    baseViews: 291,
    baseLikes: 64,
    baseComments: 12,
  },
  {
    title: 'Oracle 제출에서 실행계획을 읽을 때 먼저 보는 체크리스트',
    excerpt: 'Rows, Cost, Predicate Information, 조인 순서를 어떤 순서로 보는지 정리했습니다.',
    content:
      'Oracle 제출은 먼저 row estimate를 보고, 그다음 access predicate와 조인 순서를 확인한 뒤 마지막으로 sort와 aggregate 연산자를 비교합니다. 이렇게 보면 인덱스 튜닝 전 대부분의 회귀를 빨리 잡을 수 있습니다.',
    tags: ['Oracle', '실행계획', '튜닝'],
    category: 'discussion',
    baseViews: 244,
    baseLikes: 31,
    baseComments: 27,
  },
  {
    title: '305번에서 HASH JOIN과 Nested Loop 중 무엇이 더 나은지 질문',
    excerpt: '분기 필터의 선택도가 달라지자 조인 방식 선택도 크게 흔들렸습니다.',
    content:
      '분기 조건을 먼저 적용하니 보조 인덱스와 함께 nested loop가 가능해졌습니다. 그 전에는 초기 스캔에서 살아남는 행이 너무 많아 hash join이 더 유리했습니다.',
    tags: ['305번', 'INDEX', '실행계획', '튜닝'],
    category: 'question',
    baseViews: 327,
    baseLikes: 19,
    baseComments: 16,
    isResolved: false,
  },
  {
    title: '커뮤니티 태그 운영 가이드 업데이트',
    excerpt: '문제 번호 태그와 표준 연산자 태그를 우선 사용하면 검색이 훨씬 안정적입니다.',
    content:
      'left_join, explain, pg 같은 자주 쓰는 별칭을 대표 표기로 정규화하고 있습니다. 처음부터 표준 태그를 사용하면 검색과 필터링 품질이 더 좋아집니다.',
    tags: ['실행계획', '튜닝'],
    category: 'notice',
    baseViews: 612,
    baseLikes: 88,
    baseComments: 14,
    isPinned: true,
  },
  {
    title: '작은 조건식 수정만으로 전체 스캔을 없앤 사례',
    excerpt: '사소한 predicate rewrite 하나로 인덱스 중심 플랜으로 바뀐 경우입니다.',
    content:
      '원래 조건은 인덱스 컬럼을 함수로 감싸고 있어서 접근 경로가 나빠졌습니다. 변환을 비교식 반대편으로 옮기자 다시 선택도가 살아났습니다.',
    tags: ['INDEX', '실행계획', '튜닝'],
    category: 'tip',
    baseViews: 418,
    baseLikes: 58,
    baseComments: 9,
  },
  {
    title: '이동 평균 문제에서 WINDOW FUNCTION frame 지정이 중요한 이유',
    excerpt: '중복 값이나 성긴 타임스탬프가 있을 때 기본 frame이 의외의 결과를 만들 수 있습니다.',
    content:
      '여러 제출이 기본 frame만 쓰다가 샘플에서는 맞고 실제 케이스에서 어긋났습니다. frame을 명시하니 의도도 분명해지고 경계 케이스 회귀도 막을 수 있었습니다.',
    tags: ['WINDOW FUNCTION', 'GROUP BY', 'PostgreSQL'],
    category: 'discussion',
    baseViews: 233,
    baseLikes: 34,
    baseComments: 20,
  },
  {
    title: '정답은 맞는데 여전히 느릴 때 먼저 보는 체크리스트',
    excerpt: '출력은 맞지만 점수가 낮을 때 빠르게 병목을 찾는 순서를 정리했습니다.',
    content:
      '스캔 행 수를 먼저 보고, 필터 전에 조인이 불어났는지 확인한 뒤, 불필요한 정렬과 집계 전략이 살아남은 행 수에 맞는지 순서대로 점검합니다. 이 과정에서 대개 핵심 병목이 드러납니다.',
    tags: ['실행계획', '튜닝', 'INDEX'],
    category: 'tip',
    baseViews: 364,
    baseLikes: 71,
    baseComments: 11,
  },
  {
    title: 'PostgreSQL에서 bitmap scan과 index scan을 언제 비교하는지',
    excerpt: '중간 정도 선택도를 가진 챌린지 문제에서 유용했던 비교 기준입니다.',
    content:
      '여러 predicate가 합쳐져 중간 선택도가 나올 때는 bitmap scan이 자주 이깁니다. 다만 heap fetch 패턴이 촘촘하고 선택된 행이 잘 모여 있으면 일반 index scan이 더 빠를 때도 있습니다.',
    tags: ['PostgreSQL', 'INDEX', '실행계획'],
    category: 'question',
    baseViews: 318,
    baseLikes: 27,
    baseComments: 15,
    isResolved: true,
  },
];

const authorHandles = [
  mockCurrentHandle,
  'minseo_db',
  'junho_plan',
  'seoyoon_sql',
  'dohyun_ora',
  'haneul_exec',
  'quertimizer',
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
];

export const mockCommunitySeedLikedPostIds = ['community-001', 'community-008', 'community-015'];

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

function buildPostContentHtml(content: string, tags: string[], authorHandle: string, createdAt: string) {
  const paragraphs = content
    .split('. ')
    .map((sentence) => sentence.trim())
    .filter(Boolean)
    .map((sentence) => `<p>${sentence.endsWith('.') ? sentence : `${sentence}.`}</p>`)
    .join('');

  const tagMarkup = tags
    .slice(0, 3)
    .map((tag) => `<li>#${tag}</li>`)
    .join('');

  const shouldIncludeImage = tags.includes('실행계획') || tags.includes('INDEX') || tags.includes('튜닝');
  const imageMarkup = shouldIncludeImage
    ? `<figure class="community-detail-figure"><img src="/mock-explain.svg" alt="실행계획 메모" /><figcaption>@${authorHandle} · ${createdAt.slice(2, 16).replace('T', ' ')}</figcaption></figure>`
    : '';

  return `${paragraphs}<h2>핵심 태그</h2><ul>${tagMarkup}</ul>${imageMarkup}`;
}

export const mockCommunityPosts: CommunityPostSummary[] = Array.from({ length: 120 }, (_, index) => {
  const template = postTemplates[index % postTemplates.length];
  const cycle = Math.floor(index / postTemplates.length);
  const authorHandle = authorHandles[(index * 5) % authorHandles.length];
  const createdAt = formatSeoulIso(BASE_TIMESTAMP - index * 1000 * 60 * 95);
  const sequenceLabel = cycle > 0 ? ` · 사례 ${cycle + 1}` : '';
  const content = `${template.content} ${authorHandle} 제출과도 함께 비교하면서 트레이드오프를 확인했습니다.`;

  return {
    id: `community-${String(index + 1).padStart(3, '0')}`,
    title: `${template.title}${sequenceLabel}`,
    authorHandle,
    excerpt: template.excerpt,
    content,
    contentHtml: buildPostContentHtml(content, template.tags, authorHandle, createdAt),
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

const commentTemplates = [
  '조인 전에 row estimate가 흔들려 보입니다. 먼저 필터 선택도를 다시 확인해보세요.',
  '이 리라이트는 의도가 훨씬 분명하고 연산자 선택도 더 안정적으로 보입니다.',
  '저도 같은 문제를 겪었는데 마지막 정렬 전에 폭을 줄이니 해결됐습니다.',
  '조건을 인덱스 컬럼 쪽으로 옮기니 실행계획이 훨씬 건강해졌습니다.',
  '좋은 포인트네요. 샘플 입력이 최악 케이스를 가리고 있었던 것 같습니다.',
  'PostgreSQL과 Oracle 결과를 모두 비교해봤는데 이 해석이 둘 다 잘 맞았습니다.',
];

function getPostNumber(postId: string) {
  const matchedNumber = Number.parseInt(postId.replace(/\D/g, ''), 10);
  return Number.isNaN(matchedNumber) ? 1 : matchedNumber;
}

function createCommentTimestamp(postNumber: number, offsetMinutes: number) {
  return formatSeoulIso(BASE_TIMESTAMP - postNumber * 1000 * 60 * 73 - offsetMinutes * 60 * 1000);
}

function createReply(postNumber: number, commentIndex: number, replyIndex: number): CommunityComment {
  const authorHandle = authorHandles[(postNumber + commentIndex * 5 + replyIndex * 3) % authorHandles.length];

  return {
    id: `community-reply-${postNumber}-${commentIndex}-${replyIndex}`,
    authorHandle,
    content: `${commentTemplates[(postNumber + commentIndex + replyIndex + 2) % commentTemplates.length]} 답변 전에 플랜 모양도 한 번 더 확인했습니다.`,
    createdAt: createCommentTimestamp(postNumber, commentIndex * 23 + replyIndex * 11 + 14),
    likes: (postNumber + commentIndex + replyIndex) % 9,
    replies: [],
  };
}

function createComment(postNumber: number, commentIndex: number): CommunityComment {
  const authorHandle = authorHandles[(postNumber * 3 + commentIndex * 7) % authorHandles.length];
  const replyCount = (postNumber + commentIndex) % 3;

  return {
    id: `community-comment-${postNumber}-${commentIndex}`,
    authorHandle,
    content: `${commentTemplates[(postNumber + commentIndex) % commentTemplates.length]} 참고가 될까 해서 메모도 함께 남깁니다.`,
    createdAt: createCommentTimestamp(postNumber, commentIndex * 31 + 8),
    likes: (postNumber * 2 + commentIndex) % 14,
    replies: Array.from({ length: replyCount }, (_, replyIndex) => createReply(postNumber, commentIndex, replyIndex)),
  };
}

export function getMockCommunityPostById(postId: string) {
  return mockCommunityPosts.find((post) => post.id === postId);
}

export function getMockCommunityComments(postId: string): CommunityComment[] {
  const postNumber = getPostNumber(postId);
  const rootCommentCount = (postNumber % 4) + 2;

  return Array.from({ length: rootCommentCount }, (_, commentIndex) => createComment(postNumber, commentIndex));
}
