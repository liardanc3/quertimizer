export interface MockNotification {
  id: string;
  title: string;
  message: string;
  createdAt: string;
  href: string;
  isUnread: boolean;
}

export const mockNotifications: MockNotification[] = [
  {
    id: 'notification-001',
    title: '새 댓글이 달렸습니다',
    message: 'LEFT JOIN 질문 글에 `plan_reader`님이 답변을 남겼습니다.',
    createdAt: '2026-03-30T21:18:00+09:00',
    href: '/community/community-002',
    isUnread: true,
  },
  {
    id: 'notification-002',
    title: '좋아요를 받았습니다',
    message: '작성한 팁 글이 다른 사용자에게 좋은 반응을 얻고 있습니다.',
    createdAt: '2026-03-30T18:42:00+09:00',
    href: '/community/community-001',
    isUnread: true,
  },
  {
    id: 'notification-003',
    title: '문제 토론이 활발해졌어요',
    message: '205번 관련 토론 글에 새 답글이 이어지고 있습니다.',
    createdAt: '2026-03-29T23:05:00+09:00',
    href: '/community/community-005',
    isUnread: true,
  },
  {
    id: 'notification-004',
    title: '랭킹 변동이 있습니다',
    message: '이번 주 PostgreSQL 실행 시간 순위가 갱신되었습니다.',
    createdAt: '2026-03-29T14:30:00+09:00',
    href: '/ranking',
    isUnread: false,
  },
  {
    id: 'notification-005',
    title: '프로필 방문이 늘었습니다',
    message: '최근 활동 덕분에 프로필 조회가 꾸준히 올라가고 있습니다.',
    createdAt: '2026-03-28T10:12:00+09:00',
    href: '/profile',
    isUnread: false,
  },
];
