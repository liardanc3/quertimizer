import { mockProblems } from './problems';
import type { Profile, ProfileSolvedRecord } from '../types/domain';

const problemByNumber = new Map(mockProblems.map((problem) => [problem.number, problem]));

function createSolvedRecord(
  problemNumber: number,
  executionTimeMs: number,
  scanRows: number,
  solvedAt: string
): ProfileSolvedRecord {
  const problem = problemByNumber.get(problemNumber);

  return {
    id: `solve-${problemNumber}-${solvedAt}`,
    problemId: problem?.id ?? `profile-${problemNumber}`,
    problemNumber,
    problemTitle: problem?.title ?? `Problem ${problemNumber}`,
    executionTimeMs,
    scanRows,
    solvedAt,
  };
}

function createProfile(profile: Omit<Profile, 'solvedCount'>): Profile {
  return {
    ...profile,
    solvedCount: profile.solvedProblems.length,
  };
}

export const mockCurrentHandle = 'kim-tuner';

export const mockProfiles: Profile[] = [
  createProfile({
    handle: mockCurrentHandle,
    name: '김튜너',
    tier: 'Silver 2',
    avatarUrl: '/favicon.svg',
    bio: 'PostgreSQL 실행계획을 읽는 맛으로 푸는 SQL 러너입니다. 짧은 실행 시간과 적은 스캔 행 수를 같이 챙기는 풀이를 기록하고 있습니다.',
    links: {
      blog: 'https://blog.quertimizer.dev/kim-tuner',
      github: 'https://github.com/kim-tuner',
      email: 'kim.tuner@quertimizer.dev',
    },
    settings: {
      defaultDbms: 'postgresql',
      sqlEditorPreset: 'balanced',
      sqlVisibility: 'public',
    },
    solvedProblems: [
      createSolvedRecord(99, 15.4, 620, '2026-01-07T20:18:00+09:00'),
      createSolvedRecord(101, 12.7, 410, '2026-01-12T22:05:00+09:00'),
      createSolvedRecord(214, 18.9, 980, '2026-01-25T21:27:00+09:00'),
      createSolvedRecord(305, 16.3, 770, '2026-02-01T19:41:00+09:00'),
      createSolvedRecord(417, 14.2, 520, '2026-02-10T23:14:00+09:00'),
      createSolvedRecord(522, 11.9, 298, '2026-02-18T21:02:00+09:00'),
      createSolvedRecord(608, 20.1, 1320, '2026-02-28T20:46:00+09:00'),
      createSolvedRecord(731, 13.5, 450, '2026-03-03T18:12:00+09:00'),
      createSolvedRecord(842, 17.8, 860, '2026-03-09T21:36:00+09:00'),
      createSolvedRecord(905, 10.8, 230, '2026-03-18T20:24:00+09:00'),
      createSolvedRecord(1133, 19.6, 1440, '2026-03-24T22:11:00+09:00'),
      createSolvedRecord(1492, 12.1, 390, '2026-03-29T19:58:00+09:00'),
    ],
  }),
  createProfile({
    handle: 'park-optimizer',
    name: '박옵티마이저',
    tier: 'Diamond 1',
    bio: '힌트 없이도 실행계획이 예쁘게 나오는 쿼리를 좋아합니다. MySQL과 PostgreSQL을 번갈아 쓰며 기록을 정리합니다.',
    links: {
      github: 'https://github.com/park-optimizer',
      blog: 'https://optimizer-notes.dev',
    },
    settings: {
      defaultDbms: 'mysql',
      sqlEditorPreset: 'analysis',
      sqlVisibility: 'followers',
    },
    solvedProblems: [
      createSolvedRecord(101, 8.4, 180, '2026-02-02T14:10:00+09:00'),
      createSolvedRecord(214, 10.3, 240, '2026-02-11T16:45:00+09:00'),
      createSolvedRecord(417, 9.7, 210, '2026-02-21T21:40:00+09:00'),
      createSolvedRecord(522, 7.9, 140, '2026-03-01T10:05:00+09:00'),
      createSolvedRecord(731, 8.6, 170, '2026-03-08T13:27:00+09:00'),
      createSolvedRecord(905, 7.4, 120, '2026-03-17T19:33:00+09:00'),
      createSolvedRecord(1492, 9.1, 205, '2026-03-27T23:09:00+09:00'),
    ],
  }),
  createProfile({
    handle: 'lee-index',
    name: '이인덱스',
    tier: 'Platinum 4',
    bio: '인덱스 설계와 커버링 전략을 메모하는 중입니다. 최근에는 스캔 행 수를 절반 이하로 줄이는 연습을 하고 있어요.',
    links: {
      email: 'lee.index@quertimizer.dev',
    },
    settings: {
      defaultDbms: 'postgresql',
      sqlEditorPreset: 'focused',
      sqlVisibility: 'private',
    },
    solvedProblems: [
      createSolvedRecord(99, 13.2, 360, '2026-01-14T09:12:00+09:00'),
      createSolvedRecord(305, 17.5, 640, '2026-02-05T20:02:00+09:00'),
      createSolvedRecord(608, 15.9, 510, '2026-02-19T22:21:00+09:00'),
      createSolvedRecord(842, 12.4, 280, '2026-03-06T18:47:00+09:00'),
      createSolvedRecord(1133, 16.8, 690, '2026-03-20T11:18:00+09:00'),
    ],
  }),
];

export const mockCurrentProfile = mockProfiles.find((profile) => profile.handle === mockCurrentHandle) ?? mockProfiles[0];
export const mockProfile = mockCurrentProfile;

export function getMockProfileByHandle(handle?: string) {
  const targetHandle = handle ?? mockCurrentHandle;
  return mockProfiles.find((profile) => profile.handle === targetHandle);
}

