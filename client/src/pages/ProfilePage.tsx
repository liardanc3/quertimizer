import { Fragment, useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import { createPortal } from 'react-dom';
import {
  fetchCommunityCommentsByUser,
  fetchCommunityPostsByUser,
  fetchLikedCommentsByUser,
  fetchLikedPostsByUser,
  fetchMyCommunityComments,
  fetchMyCommunityPosts,
  fetchMyLikedComments,
  fetchMyLikedPosts,
  type ProfileCommunityComment,
  type ProfileCommunityPost,
} from '../lib/communityApi';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { createEmptySubmitHistoryPlanFilters, getExecutionPlanDetailGroups, getPlanElementButtonLabel } from '../lib/executionPlanFilters';
import { getCommunityPostPath, getLocationSearchSnapshot, getProfilePath, navigate, subscribeLocation } from '../lib/navigation';
import {
  fetchMyProfileSummary,
  fetchMySolvedProblems,
  fetchMySolvedRecords,
  fetchProfileSummary,
  fetchSolvedProblems,
  fetchSolvedRecords,
  updateMyProfile,
  type UpdateUserProfilePayload,
  type UserProfileLink,
  type UserProfileSolvedProblems,
  type UserProfileSolvedRecord,
  type UserProfileSolvedRecords,
  type UserProfileSummary,
} from '../lib/profileApi';
import { showSessionToast, syncSession, useMockSession } from '../lib/session';
import { fetchAlarms, markAlarmRead, type AlarmEntry, type AlarmPageData } from '../lib/alarmApi';
import { fetchSubmitHistories } from '../lib/submitHistoryApi';
import type { DbmsType, SubmitHistoryEntry } from '../types/domain';
import './SubmitHistoryPage.css';
import './ProfilePage.css';

interface ProfilePageProps {
  handle?: string;
}

interface ProfileEditDraft {
  bio: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
}

interface FeedbackState {
  tone: 'success' | 'error';
  message: string;
}

type ProfileSubmissionModalState =
  | { type: 'sql'; history: SubmitHistoryEntry }
  | { type: 'plan'; history: SubmitHistoryEntry }
  | null;

const profileAlarmLoadingRows = Array.from({ length: 5 }, (_, index) => index);

type SubmitHistorySqlTokenKind =
  | 'keyword'
  | 'explain-keyword'
  | 'table'
  | 'column'
  | 'string'
  | 'number'
  | 'comment'
  | 'function'
  | 'operator'
  | 'identifier';

interface SubmitHistorySqlHighlightToken {
  text: string;
  kind: SubmitHistorySqlTokenKind | null;
}

interface LinkGroupSummary {
  blog?: UserProfileLink;
  email?: UserProfileLink;
  github?: UserProfileLink;
  extras: UserProfileLink[];
}

interface ActivityHeatmapCell {
  key: string;
  label: string;
  count: number;
  submissionCount: number;
  communityCount: number;
  year: number;
}

interface RecentActivityItem {
  id: string;
  label: string;
  detail: string;
  happenedAt: string;
  href?: string;
}

interface ProfileCommunityActivityItem {
  id: string;
  kind: 'post' | 'likedPost' | 'comment' | 'likedComment';
  happenedAt: string;
  href?: string;
  title?: string;
  tags?: string[];
  excerpt?: string;
  content?: string;
}

type ProfileSection = 'summary' | 'solve' | 'community';
type ProfileTopTab = 'profile' | 'alarms';

const numberFormatter = new Intl.NumberFormat('ko-KR');
const costFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 });
const HEATMAP_DAYS = 365;
const RECENT_ACTIVITY_PAGE_SIZE = 10;
const PROFILE_SUBMISSION_PAGE_SIZE = 10;
const PROFILE_ALARM_PAGE_SIZE = 10;
const SUBMIT_HISTORY_SQL_HIGHLIGHT_KEYWORDS = new Set([
  'SELECT',
  'FROM',
  'WHERE',
  'GROUP',
  'BY',
  'ORDER',
  'HAVING',
  'LIMIT',
  'OFFSET',
  'JOIN',
  'INNER',
  'LEFT',
  'RIGHT',
  'FULL',
  'OUTER',
  'ON',
  'AS',
  'AND',
  'OR',
  'NOT',
  'IN',
  'EXISTS',
  'BETWEEN',
  'LIKE',
  'IS',
  'NULL',
  'COUNT',
  'SUM',
  'AVG',
  'MIN',
  'MAX',
  'DISTINCT',
  'CASE',
  'WHEN',
  'THEN',
  'ELSE',
  'END',
  'WITH',
  'UNION',
  'ALL',
  'EXPLAIN',
  'ANALYZE',
  'ANALYSE',
  'CREATE',
  'TEMP',
  'TABLE',
  'INSERT',
  'INTO',
  'VALUES',
  'UPDATE',
  'SET',
  'DELETE',
  'INDEX',
  'DROP',
  'ALTER',
  'ADD',
  'PRIMARY',
  'KEY',
  'FOREIGN',
  'REFERENCES',
  'UNIQUE',
  'CHECK',
  'DEFAULT',
  'PUBLIC',
  'INTEGER',
  'VARCHAR',
  'TEXT',
  'TIMESTAMP',
  'DATE',
  'BOOLEAN',
  'DECIMAL',
  'NUMERIC',
  'BIGINT',
  'SMALLINT',
  'TRUE',
  'FALSE',
]);
const SUBMIT_HISTORY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS = new Set([
  'FROM',
  'JOIN',
  'INTO',
  'UPDATE',
  'TABLE',
  'INDEX',
  'ON',
]);
const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];
const profileSections: Array<{ id: ProfileSection; label: string }> = [
  { id: 'summary', label: '최근 활동' },
  { id: 'solve', label: '문제 풀이' },
  { id: 'community', label: '커뮤니티 활동' },
];

function renderProfileSectionIcon(sectionId: ProfileSection) {
  if (sectionId === 'summary') {
    return (
      <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <circle cx="8" cy="8" r="5.25" stroke="currentColor" strokeWidth="1.45" />
        <path d="M8 4.9v3.2l2.1 1.35" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  if (sectionId === 'solve') {
    return (
      <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <rect x="3" y="2.8" width="10" height="10.4" rx="2" stroke="currentColor" strokeWidth="1.45" />
        <path d="M5.4 8.2 7.1 9.9l3.5-3.7" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.2 4.3A2.3 2.3 0 0 1 5.5 2h5A2.3 2.3 0 0 1 12.8 4.3v3.2a2.3 2.3 0 0 1-2.3 2.3H8.9l-2.6 2.1v-2.1H5.5a2.3 2.3 0 0 1-2.3-2.3V4.3Z" stroke="currentColor" strokeWidth="1.45" strokeLinejoin="round" />
    </svg>
  );
}
const emptySolvedProblems: UserProfileSolvedProblems = {
  solvedProblemCount: 0,
  solvedProblemIds: [],
};
const emptySolvedRecords: UserProfileSolvedRecords = {
  solvedRecords: [],
};
const emptyPlanFiltersByDbms = {
  postgresql: createEmptySubmitHistoryPlanFilters(),
  oracle: createEmptySubmitHistoryPlanFilters(),
};
const emptyProfileAlarmPage: AlarmPageData = {
  currentPage: 1,
  pageSize: PROFILE_ALARM_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  unreadCount: 0,
  alarms: [],
};

function createEditDraft(profile: UserProfileSummary): ProfileEditDraft {
  return {
    bio: profile.bio,
    links: profile.links.length > 0 ? profile.links : [{ type: '', value: '' }],
    defaultDbms: profile.defaultDbms,
    sqlPublic: profile.sqlPublic,
    executionPercentilePublic: profile.executionPercentilePublic,
    solvedRecordsPublic: profile.solvedRecordsPublic,
    solvedProblemCountPublic: profile.solvedProblemCountPublic,
  };
}

function normalizeLinksForSave(links: UserProfileLink[]) {
  return links
    .map((link) => ({
      type: link.type.trim(),
      value: link.value.trim(),
    }))
    .filter((link) => link.type !== '' && link.value !== '');
}

function formatDateTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function formatPercentile(value: number | null) {
  if (value == null) {
    return '비공개';
  }

  return `${Math.round(value * 10) / 10}%`;
}

function formatDbmsLabel(dbms: DbmsType) {
  return dbms === 'oracle' ? 'Oracle' : 'PostgreSQL';
}

function formatCost(value: number) {
  return costFormatter.format(Math.round(value * 10) / 10);
}

function buildTextSnippet(value: string, maxLength: number) {
  const normalizedValue = value.replace(/\s+/g, ' ').trim();

  if (normalizedValue.length <= maxLength) {
    return normalizedValue;
  }

  return `${normalizedValue.slice(0, maxLength).trimEnd()}...`;
}

function createFallbackAvatarLabel(handle: string) {
  const trimmedHandle = handle.trim();

  if (trimmedHandle === '') {
    return '?';
  }

  return trimmedHandle.charAt(0).toUpperCase();
}

function classifyLinkGroups(links: UserProfileLink[]): LinkGroupSummary {
  const summary: LinkGroupSummary = { extras: [] };

  links.forEach((link) => {
    const normalizedType = link.type.trim().toLowerCase();

    if (summary.blog == null && (normalizedType === 'blog' || normalizedType.includes('blog'))) {
      summary.blog = link;
      return;
    }

    if (summary.email == null && (normalizedType === 'email' || normalizedType.includes('mail'))) {
      summary.email = link;
      return;
    }

    if (summary.github == null && normalizedType.includes('github')) {
      summary.github = link;
      return;
    }

    summary.extras.push(link);
  });

  return summary;
}

function resolveLinkHref(link: UserProfileLink) {
  const normalizedType = link.type.trim().toLowerCase();
  const normalizedValue = link.value.trim();

  if (normalizedType.includes('mail') && !normalizedValue.startsWith('mailto:')) {
    return `mailto:${normalizedValue}`;
  }

  if (normalizedValue.startsWith('http://') || normalizedValue.startsWith('https://') || normalizedValue.startsWith('mailto:')) {
    return normalizedValue;
  }

  return `https://${normalizedValue}`;
}

function createHeatmapCells(year: number, submissionDates: string[], communityDates: string[]) {
  const submissionCountByDate = new Map<string, number>();
  const communityCountByDate = new Map<string, number>();

  submissionDates.forEach((value) => {
    const date = new Date(value);

    if (Number.isNaN(date.getTime()) || date.getFullYear() != year) {
      return;
    }

    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    submissionCountByDate.set(key, (submissionCountByDate.get(key) ?? 0) + 1);
  });

  communityDates.forEach((value) => {
    const date = new Date(value);

    if (Number.isNaN(date.getTime()) || date.getFullYear() != year) {
      return;
    }

    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    communityCountByDate.set(key, (communityCountByDate.get(key) ?? 0) + 1);
  });

  const firstDay = new Date(year, 0, 1);
  const lastDay = new Date(year, 11, 31);
  const totalDays = Math.floor((lastDay.getTime() - firstDay.getTime()) / (24 * 60 * 60 * 1000)) + 1;

  return Array.from({ length: totalDays }, (_, index) => {
    const currentDate = new Date(year, 0, 1 + index);
    const key = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`;
    const submissionCount = submissionCountByDate.get(key) ?? 0;
    const communityCount = communityCountByDate.get(key) ?? 0;

    return {
      key,
      label: `${key} 문제 제출 ${submissionCount}건 · 커뮤니티 활동 ${communityCount}건`,
      count: submissionCount + communityCount,
      submissionCount,
      communityCount,
      year,
    } satisfies ActivityHeatmapCell;
  });
}

function getHeatmapTone(count: number) {
  if (count >= 4) {
    return 'is-level-4';
  }

  if (count === 3) {
    return 'is-level-3';
  }

  if (count === 2) {
    return 'is-level-2';
  }

  if (count === 1) {
    return 'is-level-1';
  }

  return 'is-level-0';
}

function createSortedUniqueDateSelection(dates: string[], orderedCellKeys: string[]) {
  const uniqueDateSet = new Set(dates);
  return orderedCellKeys.filter((key) => uniqueDateSet.has(key));
}

function SelectionCheckbox({ checked }: { checked: boolean }) {
  return <span className={`runtime-check-indicator ${checked ? 'is-checked' : ''}`} aria-hidden="true" />;
}

function formatSubmittedAt(value: string) {
  if (value.trim() === '') {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return `${parsedDate.getFullYear()}-${String(parsedDate.getMonth() + 1).padStart(2, '0')}-${String(parsedDate.getDate()).padStart(2, '0')} ${String(parsedDate.getHours()).padStart(2, '0')}:${String(parsedDate.getMinutes()).padStart(2, '0')}:${String(parsedDate.getSeconds()).padStart(2, '0')}`;
}

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'oracle' ? 'Oracle' : 'PostgreSQL';
}

function buildProblemLabel(problemId: string) {
  return `문제 ${problemId}`;
}

function readProfileTopTab(search: string, isOwnProfile: boolean): ProfileTopTab {
  if (!isOwnProfile) {
    return 'profile';
  }

  return new URLSearchParams(search).get('tab') === 'alarms' ? 'alarms' : 'profile';
}

function truncateAlarmHoverText(value: string) {
  const normalizedValue = value.trim();
  if (normalizedValue.length <= 15) {
    return normalizedValue;
  }

  return `${normalizedValue.slice(0, 15)}...`;
}

function tokenizeSubmitHistorySqlLine(line: string) {
  const tokens: SubmitHistorySqlHighlightToken[] = [];
  const tokenPattern =
    /--.*$|'(?:''|[^'])*'|"(?:["]|[^"])*"|[A-Za-z_][A-Za-z0-9_$]*|\d+(?:\.\d+)?|<=|>=|<>|!=|==|[=<>+\-*/%]+|[(),.;]|\s+|./g;
  const lineTokens = Array.from(line.matchAll(tokenPattern), (match) => match[0]);
  let expectTable = false;

  for (let index = 0; index < lineTokens.length; index += 1) {
    const token = lineTokens[index];

    if (/^\s+$/.test(token)) {
      tokens.push({ text: token, kind: null });
      continue;
    }

    if (token.startsWith('--')) {
      tokens.push({ text: token, kind: 'comment' });
      break;
    }

    if (/^'(?:''|[^'])*'$/.test(token) || /^"(?:["]|[^"])*"$/.test(token)) {
      tokens.push({ text: token, kind: 'string' });
      expectTable = false;
      continue;
    }

    if (/^\d+(?:\.\d+)?$/.test(token)) {
      tokens.push({ text: token, kind: 'number' });
      continue;
    }

    if (/^[(),.;]$/.test(token) || /^[=<>+\-*/%]+$/.test(token)) {
      tokens.push({ text: token, kind: 'operator' });
      if (token !== ',') {
        expectTable = false;
      }
      continue;
    }

    if (/^[A-Za-z_][A-Za-z0-9_$]*$/.test(token)) {
      const upperToken = token.toUpperCase();
      const previousMeaningfulToken = [...lineTokens.slice(0, index)]
        .reverse()
        .find((candidate) => !/^\s+$/.test(candidate));
      const nextMeaningfulToken = lineTokens.slice(index + 1).find((candidate) => !/^\s+$/.test(candidate));

      if (SUBMIT_HISTORY_SQL_HIGHLIGHT_KEYWORDS.has(upperToken)) {
        tokens.push({
          text: token,
          kind:
            upperToken === 'EXPLAIN' || upperToken === 'ANALYZE' || upperToken === 'ANALYSE'
              ? 'explain-keyword'
              : 'keyword',
        });
        expectTable = SUBMIT_HISTORY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS.has(upperToken);
        continue;
      }

      if (previousMeaningfulToken === '.') {
        tokens.push({ text: token, kind: 'column' });
        expectTable = false;
        continue;
      }

      if (expectTable) {
        tokens.push({ text: token, kind: 'table' });
        expectTable = false;
        continue;
      }

      if (nextMeaningfulToken === '(') {
        tokens.push({ text: token, kind: 'function' });
        expectTable = false;
        continue;
      }

      tokens.push({ text: token, kind: 'identifier' });
      expectTable = false;
      continue;
    }

    tokens.push({ text: token, kind: null });
  }

  return tokens;
}

function renderSubmitHistoryHighlightedSql(sql: string) {
  const normalizedSql = sql.replace(/\r\n?/g, '\n');
  const lines = normalizedSql.split('\n');

  return lines.map((line, lineIndex) => {
    const lineTokens = tokenizeSubmitHistorySqlLine(line);

    return (
      <Fragment key={`line-${lineIndex}`}>
        {lineTokens.map((token, tokenIndex) =>
          token.kind == null ? (
            <span key={`token-${lineIndex}-${tokenIndex}`}>{token.text}</span>
          ) : (
            <span key={`token-${lineIndex}-${tokenIndex}`} className={`solve-sql-token is-${token.kind}`}>
              {token.text}
            </span>
          ),
        )}
        {lineIndex < lines.length - 1 ? '\n' : null}
      </Fragment>
    );
  });
}

async function fetchAllSubmitHistoriesForUser(handle: string) {
  const requestPage = (page: number) =>
    fetchSubmitHistories({
      page,
      submitId: '',
      query: handle,
      dbms: 'all',
      problemId: '',
      judge: 'all',
      costSort: 'none',
      planFiltersByDbms: emptyPlanFiltersByDbms,
    });

  const firstPage = await requestPage(1);
  const histories = [...firstPage.histories.filter((history) => history.handle === handle)];

  if (firstPage.totalPages > 1) {
    const remainingPages = await Promise.all(
      Array.from({ length: firstPage.totalPages - 1 }, (_, index) => requestPage(index + 2)),
    );

    remainingPages.forEach((pageData) => {
      histories.push(...pageData.histories.filter((history) => history.handle === handle));
    });
  }

  return histories.sort((left, right) => new Date(right.submittedAt).getTime() - new Date(left.submittedAt).getTime());
}

function createHeatmapYears(submissions: SubmitHistoryEntry[]) {
  const currentYear = new Date().getFullYear();
  const firstSubmissionYear = submissions.reduce((minimumYear, submission) => {
    const submittedAt = new Date(submission.submittedAt);

    if (Number.isNaN(submittedAt.getTime())) {
      return minimumYear;
    }

    return Math.min(minimumYear, submittedAt.getFullYear());
  }, currentYear);

  return Array.from({ length: currentYear - firstSubmissionYear + 1 }, (_, index) => firstSubmissionYear + index);
}

function buildRecentActivities(
  submissions: SubmitHistoryEntry[],
  solvedRecords: UserProfileSolvedRecord[],
  posts: ProfileCommunityPost[],
  likedPosts: ProfileCommunityPost[],
  comments: ProfileCommunityComment[],
  likedComments: ProfileCommunityComment[],
) {
  const activities: RecentActivityItem[] = [
    ...submissions.map((history) => ({
      id: `submit-${history.submitId}`,
      label: history.success ? `문제 ${history.problemId}를 정답 제출했다` : `문제 ${history.problemId}를 제출했다`,
      detail: `${formatDbmsLabel(history.dbms)} · ${formatDateTime(history.submittedAt)}`,
      happenedAt: history.submittedAt,
      href: `/problems/${history.problemId}`,
    })),
    ...solvedRecords.map((record) => ({
      id: `solve-${record.problemId}-${record.submittedAt}`,
      label: `문제 ${record.problemId}를 해결했다`,
      detail: `${record.problemTitle} · ${formatDateTime(record.submittedAt)}`,
      happenedAt: record.submittedAt,
      href: `/problems/${record.problemId}`,
    })),
    ...posts.map((post) => ({
      id: `post-${post.postId}`,
      label: `게시글 ${post.title}을 작성했다`,
      detail: formatDateTime(post.createdAt),
      happenedAt: post.createdAt,
      href: getCommunityPostPath(post.postId),
    })),
    ...likedPosts.map((post) => ({
      id: `post-like-${post.postId}-${post.createdAt}`,
      label: `게시글 ${post.title}에 좋아요를 남겼다`,
      detail: formatDateTime(post.createdAt),
      happenedAt: post.createdAt,
      href: getCommunityPostPath(post.postId),
    })),
    ...comments.map((comment) => ({
      id: `comment-${comment.commentId}`,
      label: `게시글 ${comment.postTitle}에 댓글을 남겼다`,
      detail: formatDateTime(comment.createdAt),
      happenedAt: comment.createdAt,
      href: getCommunityPostPath(comment.postId),
    })),
    ...likedComments.map((comment) => ({
      id: `comment-like-${comment.commentId}-${comment.createdAt}`,
      label: `게시글 ${comment.postTitle}의 댓글에 좋아요를 눌렀다`,
      detail: formatDateTime(comment.createdAt),
      happenedAt: comment.createdAt,
      href: getCommunityPostPath(comment.postId),
    })),
  ];

  return activities.sort((left, right) => new Date(right.happenedAt).getTime() - new Date(left.happenedAt).getTime());
}

function buildCommunityActivities(
  posts: ProfileCommunityPost[],
  likedPosts: ProfileCommunityPost[],
  comments: ProfileCommunityComment[],
  likedComments: ProfileCommunityComment[],
) {
  return [
    ...posts.map((post) => ({
      id: `community-post-${post.postId}-${post.createdAt}`,
      kind: 'post' as const,
      happenedAt: post.createdAt,
      href: getCommunityPostPath(post.postId),
      title: post.title,
      tags: post.tags,
      excerpt: post.excerpt,
    })),
    ...likedPosts.map((post) => ({
      id: `community-liked-post-${post.postId}-${post.createdAt}`,
      kind: 'likedPost' as const,
      happenedAt: post.createdAt,
      href: getCommunityPostPath(post.postId),
      title: post.title,
    })),
    ...comments.map((comment) => ({
      id: `community-comment-${comment.commentId}-${comment.createdAt}`,
      kind: 'comment' as const,
      happenedAt: comment.createdAt,
      href: getCommunityPostPath(comment.postId),
      title: comment.postTitle,
      content: comment.content,
    })),
    ...likedComments.map((comment) => ({
      id: `community-liked-comment-${comment.commentId}-${comment.createdAt}`,
      kind: 'likedComment' as const,
      happenedAt: comment.createdAt,
      href: getCommunityPostPath(comment.postId),
      content: comment.content,
    })),
  ].sort((left, right) => new Date(right.happenedAt).getTime() - new Date(left.happenedAt).getTime()) satisfies ProfileCommunityActivityItem[];
}

function ProfileStatePage({ label, title, description }: { label: string; title: string; description: string }) {
  return (
    <div className="page-stack profile-page submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card profile-tab-shell">
        <div className="problem-toolbar submit-history-toolbar-stack profile-tab-toolbar">
          <div className="solve-dbms-tab-row profile-handle-tab-row" aria-hidden="true">
            <span className="solve-dbms-tab is-selected">{label}</span>
          </div>
        </div>
      </section>

      <section className="panel-card profile-empty-panel">
        <p className="panel-meta">프로필</p>
        <h1 className="page-title">{title}</h1>
        <p className="muted-text">{description}</p>
      </section>
    </div>
  );
}

function ProfileLoadingShell({ label }: { label: string }) {
  return (
    <div className="page-stack profile-page submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card profile-tab-shell">
        <div className="problem-toolbar submit-history-toolbar-stack profile-tab-toolbar">
          <div className="solve-dbms-tab-row profile-handle-tab-row" role="tablist" aria-label="프로필 Handle">
            <span className="solve-dbms-tab is-selected" role="tab" aria-selected={true}>
              {label}
            </span>
          </div>
        </div>
      </section>

      <section className="panel-card profile-hero-panel">
        <div className="profile-hero-layout-next">
          <div className="profile-hero-avatar-shell">
            <div className="profile-hero-avatar">{createFallbackAvatarLabel(label)}</div>
          </div>

          <div className="profile-hero-copy-next">
            <div>
              <h1 className="page-title profile-page-title">{label}</h1>
            </div>

            <div className="profile-loading-copy" aria-hidden="true">
              <span className="profile-loading-placeholder is-bio" />
              <span className="profile-loading-placeholder is-bio-short" />
            </div>

            <div className="profile-hero-link-grid profile-loading-link-grid" aria-hidden="true">
              <span className="profile-hero-link-card profile-loading-link-card">
                <span className="profile-loading-placeholder is-link-label" />
                <span className="profile-loading-placeholder is-link-value" />
              </span>
              <span className="profile-hero-link-card profile-loading-link-card">
                <span className="profile-loading-placeholder is-link-label" />
                <span className="profile-loading-placeholder is-link-value" />
              </span>
              <span className="profile-hero-link-card profile-loading-link-card">
                <span className="profile-loading-placeholder is-link-label" />
                <span className="profile-loading-placeholder is-link-value" />
              </span>
            </div>
          </div>

        </div>
      </section>

      <section className="panel-card profile-main-shell is-loading">
        <div className="profile-main-grid-next profile-loading-main-grid" aria-hidden="true">
          <aside className="profile-side-nav">
            <span className="profile-side-nav-item is-selected profile-loading-nav-item">
              <strong>최근 활동</strong>
            </span>
            <span className="profile-side-nav-item profile-loading-nav-item">
              <strong>문제 풀이</strong>
            </span>
            <span className="profile-side-nav-item profile-loading-nav-item">
              <strong>커뮤니티 활동</strong>
            </span>
          </aside>

          <div className="profile-main-content profile-loading-content">
            <div className="profile-summary-grid-layout">
              <section className="panel-card profile-summary-card profile-heatmap-card profile-summary-plain-card">
                <div className="profile-section-heading-row profile-heatmap-heading-row">
                  <div className="solve-dbms-tab-row profile-heatmap-year-tabs" aria-hidden="true">
                    <span className="solve-dbms-tab profile-heatmap-year-tab is-selected">{new Date().getFullYear()}</span>
                  </div>
                </div>
                <div className="profile-heatmap-layout" aria-hidden="true">
                  <div className="profile-heatmap-grid">
                    {Array.from({ length: 365 }, (_, index) => (
                      <span key={`heatmap-${index}`} className="profile-heatmap-cell is-level-0" />
                    ))}
                  </div>
                </div>
              </section>
            </div>

            <section className="panel-card profile-summary-card profile-recent-card profile-summary-plain-card">
              <div className="profile-section-heading-row profile-recent-heading-row">
                <h2 className="profile-section-title">최근 활동</h2>
              </div>
              <div className="profile-recent-activity-list">
                {Array.from({ length: 4 }, (_, index) => (
                  <span key={`activity-${index}`} className="profile-recent-activity-item is-static profile-loading-activity-item">
                    <span className="profile-loading-placeholder is-activity-title" />
                    <span className="profile-loading-placeholder is-activity-detail" />
                  </span>
                ))}
              </div>
            </section>
          </div>
        </div>

        <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
          <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
        </div>
      </section>
    </div>
  );
}

export default function ProfilePage({ handle: profileHandle }: ProfilePageProps) {
  const { isAuthenticated, isReady, handle: currentHandle } = useMockSession();
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const [profileSummary, setProfileSummary] = useState<UserProfileSummary | null>(null);
  const [lastViewedHandle, setLastViewedHandle] = useState<string | null>(profileHandle ?? null);
  const [solvedProblems, setSolvedProblems] = useState<UserProfileSolvedProblems>(emptySolvedProblems);
  const [solvedRecords, setSolvedRecords] = useState<UserProfileSolvedRecords>(emptySolvedRecords);
  const [authoredPosts, setAuthoredPosts] = useState<ProfileCommunityPost[]>([]);
  const [likedPosts, setLikedPosts] = useState<ProfileCommunityPost[]>([]);
  const [communityComments, setCommunityComments] = useState<ProfileCommunityComment[]>([]);
  const [likedComments, setLikedComments] = useState<ProfileCommunityComment[]>([]);
  const [recentSubmissions, setRecentSubmissions] = useState<SubmitHistoryEntry[]>([]);
  const [profileAlarmPageData, setProfileAlarmPageData] = useState<AlarmPageData>(emptyProfileAlarmPage);
  const [isProfileAlarmLoading, setIsProfileAlarmLoading] = useState(false);
  const [profileAlarmErrorMessage, setProfileAlarmErrorMessage] = useState<string | null>(null);
  const [profileAlarmPage, setProfileAlarmPage] = useState(1);
  const [profileAlarmPageDraft, setProfileAlarmPageDraft] = useState('1');
  const [isProfileAlarmPageEditing, setIsProfileAlarmPageEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<ProfileSection>('summary');
  const [selectedHeatmapYear, setSelectedHeatmapYear] = useState(new Date().getFullYear());
  const [selectedHeatmapDates, setSelectedHeatmapDates] = useState<string[]>([]);
  const [heatmapSelectionAnchor, setHeatmapSelectionAnchor] = useState<string | null>(null);
  const [recentActivityPage, setRecentActivityPage] = useState(1);
  const [recentActivityPageDraft, setRecentActivityPageDraft] = useState('1');
  const [isRecentActivityPageEditing, setIsRecentActivityPageEditing] = useState(false);
  const [solveSubmissionPage, setSolveSubmissionPage] = useState(1);
  const [solveSubmissionPageDraft, setSolveSubmissionPageDraft] = useState('1');
  const [isSolveSubmissionPageEditing, setIsSolveSubmissionPageEditing] = useState(false);
  const [solveModalState, setSolveModalState] = useState<ProfileSubmissionModalState>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editDraft, setEditDraft] = useState<ProfileEditDraft | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const resolvedProfileId = profileHandle ?? currentHandle ?? lastViewedHandle;
  const isOwnProfile = isAuthenticated && currentHandle != null && resolvedProfileId === currentHandle;
  const profileBasePath =
    profileHandle != null
      ? getProfilePath(profileHandle)
      : isOwnProfile
        ? getProfilePath()
        : resolvedProfileId != null
          ? getProfilePath(resolvedProfileId)
          : getProfilePath();
  const activeTopTab = readProfileTopTab(locationSearch || window.location.search, isOwnProfile);
  const isAlarmListOpen = activeTopTab === 'alarms';

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!resolvedProfileId) {
      setProfileSummary(null);
      setSolvedProblems(emptySolvedProblems);
      setSolvedRecords(emptySolvedRecords);
      setAuthoredPosts([]);
      setLikedPosts([]);
      setCommunityComments([]);
      setLikedComments([]);
      setRecentSubmissions([]);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    let cancelled = false;

    setIsLoading(true);
    setErrorMessage(null);
    setFeedback(null);
    setIsEditOpen(false);

    const profileSummaryRequest = isOwnProfile ? fetchMyProfileSummary() : fetchProfileSummary(resolvedProfileId);
    const solvedProblemsRequest = isOwnProfile ? fetchMySolvedProblems() : fetchSolvedProblems(resolvedProfileId);
    const solvedRecordsRequest = isOwnProfile ? fetchMySolvedRecords() : fetchSolvedRecords(resolvedProfileId);
    const postsRequest = isOwnProfile ? fetchMyCommunityPosts() : fetchCommunityPostsByUser(resolvedProfileId);
    const likedPostsRequest = isOwnProfile ? fetchMyLikedPosts() : fetchLikedPostsByUser(resolvedProfileId);
    const commentsRequest = isOwnProfile ? fetchMyCommunityComments() : fetchCommunityCommentsByUser(resolvedProfileId);
    const likedCommentsRequest = isOwnProfile ? fetchMyLikedComments() : fetchLikedCommentsByUser(resolvedProfileId);
    const recentSubmissionsRequest = fetchAllSubmitHistoriesForUser(resolvedProfileId);

    Promise.allSettled([
      profileSummaryRequest,
      solvedProblemsRequest,
      solvedRecordsRequest,
      postsRequest,
      likedPostsRequest,
      commentsRequest,
      likedCommentsRequest,
      recentSubmissionsRequest,
    ]).then((results) => {
      if (cancelled) {
        return;
      }

      const [
        summaryResult,
        solvedProblemsResult,
        solvedRecordsResult,
        postsResult,
        likedPostsResult,
        commentsResult,
        likedCommentsResult,
        recentSubmissionsResult,
      ] = results;

      if (summaryResult.status === 'rejected') {
        setProfileSummary(null);
        setEditDraft(null);
        setErrorMessage(summaryResult.reason instanceof Error ? summaryResult.reason.message : '프로필을 불러오지 못했다.');
        setSolvedProblems(emptySolvedProblems);
        setSolvedRecords(emptySolvedRecords);
        setAuthoredPosts([]);
        setLikedPosts([]);
        setCommunityComments([]);
        setLikedComments([]);
        setRecentSubmissions([]);
        setIsLoading(false);
        return;
      }

      setProfileSummary(summaryResult.value);
      setLastViewedHandle(summaryResult.value.handle);
      setEditDraft(createEditDraft(summaryResult.value));
      setSolvedProblems(solvedProblemsResult.status === 'fulfilled' ? solvedProblemsResult.value : emptySolvedProblems);
      setSolvedRecords(solvedRecordsResult.status === 'fulfilled' ? solvedRecordsResult.value : emptySolvedRecords);
      setAuthoredPosts(postsResult.status === 'fulfilled' ? postsResult.value : []);
      setLikedPosts(likedPostsResult.status === 'fulfilled' ? likedPostsResult.value : []);
      setCommunityComments(commentsResult.status === 'fulfilled' ? commentsResult.value : []);
      setLikedComments(likedCommentsResult.status === 'fulfilled' ? likedCommentsResult.value : []);
      setRecentSubmissions(recentSubmissionsResult.status === 'fulfilled' ? recentSubmissionsResult.value : []);
      setIsLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [isOwnProfile, isReady, resolvedProfileId]);

  const visibleLinks = profileSummary?.links ?? [];
  const linkGroups = useMemo(() => classifyLinkGroups(visibleLinks), [visibleLinks]);
  const heroLinks = useMemo(
    () => [
      linkGroups.blog ? { key: 'blog', label: 'Blog', value: linkGroups.blog.value, href: resolveLinkHref(linkGroups.blog) } : null,
      linkGroups.email ? { key: 'email', label: 'Email', value: linkGroups.email.value, href: resolveLinkHref(linkGroups.email) } : null,
      linkGroups.github ? { key: 'github', label: 'GitHub', value: linkGroups.github.value, href: resolveLinkHref(linkGroups.github) } : null,
      ...linkGroups.extras.map((link, index) => ({
        key: `extra-${index}-${link.type}-${link.value}`,
        label: link.type,
        value: link.value,
        href: resolveLinkHref(link),
      })),
    ].filter((link): link is { key: string; label: string; value: string; href: string } => link != null),
    [linkGroups],
  );
  const showSolvedProblemSection = isOwnProfile || profileSummary?.solvedProblemCountPublic === true;
  const showSolvedRecordsSection = isOwnProfile || profileSummary?.solvedRecordsPublic === true;
  const showSubmissionSection = isOwnProfile || profileSummary?.sqlPublic === true;
  const heatmapCells = useMemo(
    () =>
      createHeatmapCells(
        selectedHeatmapYear,
        recentSubmissions.map((history) => history.submittedAt),
        [
          ...authoredPosts.map((post) => post.createdAt),
          ...likedPosts.map((post) => post.createdAt),
          ...communityComments.map((comment) => comment.createdAt),
          ...likedComments.map((comment) => comment.createdAt),
        ],
      ),
    [authoredPosts, communityComments, likedComments, likedPosts, recentSubmissions, selectedHeatmapYear],
  );
  const heatmapYears = useMemo(() => createHeatmapYears(recentSubmissions), [recentSubmissions]);
  const recentActivities = useMemo(
    () => buildRecentActivities(recentSubmissions, solvedRecords.solvedRecords, authoredPosts, likedPosts, communityComments, likedComments),
    [authoredPosts, communityComments, likedComments, likedPosts, recentSubmissions, solvedRecords.solvedRecords],
  );
  const filteredRecentActivities = useMemo(
    () =>
      selectedHeatmapDates.length === 0
        ? recentActivities
        : recentActivities.filter((activity) => selectedHeatmapDates.some((date) => activity.happenedAt.startsWith(date))),
    [recentActivities, selectedHeatmapDates],
  );
  const recentActivityTotalPages = Math.max(1, Math.ceil(filteredRecentActivities.length / RECENT_ACTIVITY_PAGE_SIZE));
  const pagedRecentActivities = useMemo(() => {
    const normalizedPage = Math.min(recentActivityPage, recentActivityTotalPages);
    const fromIndex = (normalizedPage - 1) * RECENT_ACTIVITY_PAGE_SIZE;
    return filteredRecentActivities.slice(fromIndex, fromIndex + RECENT_ACTIVITY_PAGE_SIZE);
  }, [filteredRecentActivities, recentActivityPage, recentActivityTotalPages]);
  const solveSubmissionTotalPages = Math.max(1, Math.ceil(recentSubmissions.length / PROFILE_SUBMISSION_PAGE_SIZE));
  const pagedSolveSubmissions = useMemo(() => {
    const normalizedPage = Math.min(solveSubmissionPage, solveSubmissionTotalPages);
    const fromIndex = (normalizedPage - 1) * PROFILE_SUBMISSION_PAGE_SIZE;
    return recentSubmissions.slice(fromIndex, fromIndex + PROFILE_SUBMISSION_PAGE_SIZE);
  }, [recentSubmissions, solveSubmissionPage, solveSubmissionTotalPages]);
  const highlightedSolveModalSql = useMemo(() => {
    if (solveModalState?.type !== 'sql') {
      return null;
    }

    return renderSubmitHistoryHighlightedSql(solveModalState.history.submittedSql);
  }, [solveModalState]);
  const solvedProblemIds = useMemo(() => [...solvedProblems.solvedProblemIds], [solvedProblems.solvedProblemIds]);
  const failedProblemIds = useMemo(() => {
    const uniqueIds = new Set<string>();
    recentSubmissions.forEach((history) => {
      if (!history.success) {
        uniqueIds.add(history.problemId);
      }
    });
    return [...uniqueIds];
  }, [recentSubmissions]);
  const communityActivities = useMemo(
    () => buildCommunityActivities(authoredPosts, likedPosts, communityComments, likedComments),
    [authoredPosts, likedPosts, communityComments, likedComments],
  );

  useEffect(() => {
    if (!isRecentActivityPageEditing) {
      setRecentActivityPageDraft(String(Math.min(recentActivityPage, recentActivityTotalPages)));
    }
  }, [isRecentActivityPageEditing, recentActivityPage, recentActivityTotalPages]);

  useEffect(() => {
    if (!isSolveSubmissionPageEditing) {
      setSolveSubmissionPageDraft(String(Math.min(solveSubmissionPage, solveSubmissionTotalPages)));
    }
  }, [isSolveSubmissionPageEditing, solveSubmissionPage, solveSubmissionTotalPages]);

  useEffect(() => {
    if (recentActivityPage > recentActivityTotalPages) {
      setRecentActivityPage(recentActivityTotalPages);
    }
  }, [recentActivityPage, recentActivityTotalPages]);

  useEffect(() => {
    if (solveSubmissionPage > solveSubmissionTotalPages) {
      setSolveSubmissionPage(solveSubmissionTotalPages);
    }
  }, [solveSubmissionPage, solveSubmissionTotalPages]);

  useEffect(() => {
    setSelectedHeatmapYear(new Date().getFullYear());
    setSelectedHeatmapDates([]);
    setHeatmapSelectionAnchor(null);
    setRecentActivityPage(1);
    setRecentActivityPageDraft('1');
    setIsRecentActivityPageEditing(false);
    setSolveSubmissionPage(1);
    setSolveSubmissionPageDraft('1');
    setIsSolveSubmissionPageEditing(false);
    setProfileAlarmPage(1);
    setProfileAlarmPageDraft('1');
    setIsProfileAlarmPageEditing(false);
    setProfileAlarmPageData(emptyProfileAlarmPage);
    setProfileAlarmErrorMessage(null);
  }, [resolvedProfileId]);

  useEffect(() => {
    if (heatmapYears.length === 0) {
      return;
    }

    if (!heatmapYears.includes(selectedHeatmapYear)) {
      setSelectedHeatmapYear(heatmapYears[heatmapYears.length - 1]);
    }
  }, [heatmapYears, selectedHeatmapYear]);

  useEffect(() => {
    if (solveModalState == null) {
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setSolveModalState(null);
      }
    }

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [solveModalState]);

  useEffect(() => {
    setSelectedHeatmapDates([]);
    setHeatmapSelectionAnchor(null);
    setRecentActivityPage(1);
    setRecentActivityPageDraft('1');
    setIsRecentActivityPageEditing(false);
  }, [selectedHeatmapYear]);

  useEffect(() => {
    if (!isOwnProfile) {
      setProfileAlarmPageData(emptyProfileAlarmPage);
      setProfileAlarmErrorMessage(null);
      return;
    }

    if (!isAlarmListOpen) {
      return;
    }

    let cancelled = false;
    setIsProfileAlarmLoading(true);
    setProfileAlarmErrorMessage(null);

    fetchAlarms(profileAlarmPage, PROFILE_ALARM_PAGE_SIZE)
      .then((nextAlarmPage) => {
        if (cancelled) {
          return;
        }

        setProfileAlarmPageData(nextAlarmPage);
        if (nextAlarmPage.currentPage !== profileAlarmPage) {
          setProfileAlarmPage(nextAlarmPage.currentPage);
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setProfileAlarmErrorMessage(error instanceof Error ? error.message : '알림 목록을 불러오지 못했다.');
      })
      .finally(() => {
        if (!cancelled) {
          setIsProfileAlarmLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isAlarmListOpen, isOwnProfile, profileAlarmPage]);

  useEffect(() => {
    if (!isProfileAlarmPageEditing) {
      setProfileAlarmPageDraft(String(profileAlarmPageData.currentPage));
    }
  }, [isProfileAlarmPageEditing, profileAlarmPageData.currentPage]);

  useEffect(() => {
    if (profileAlarmPage > profileAlarmPageData.totalPages) {
      setProfileAlarmPage(profileAlarmPageData.totalPages);
    }
  }, [profileAlarmPage, profileAlarmPageData.totalPages]);

  if (!isReady || isLoading) {
    return <ProfileLoadingShell label={profileSummary?.handle ?? resolvedProfileId ?? currentHandle ?? '프로필'} />;
  }

  if (!resolvedProfileId) {
    return <ProfileStatePage label="프로필" title="조회할 프로필이 없다." description="로그인 후 내 프로필을 열거나 Handle 경로로 접근해라." />;
  }

  if (!profileSummary) {
    return (
      <div className="page-stack profile-page submit-history-page home-page">
        <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card profile-tab-shell">
          <div className="problem-toolbar submit-history-toolbar-stack profile-tab-toolbar">
            <div className="solve-dbms-tab-row profile-handle-tab-row" aria-hidden="true">
              <span className="solve-dbms-tab is-selected">{resolvedProfileId}</span>
            </div>
          </div>
        </section>

        <section className="panel-card profile-empty-panel">
          <PageLoadFailureState />
        </section>
      </div>
    );
  }

  function updateDraft(updater: (draft: ProfileEditDraft) => ProfileEditDraft) {
    setEditDraft((currentDraft) => (currentDraft ? updater(currentDraft) : currentDraft));
  }

  function applyRecentActivityPageJump() {
    const parsedPage = Number.parseInt(recentActivityPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? recentActivityPage
      : Math.min(recentActivityTotalPages, Math.max(1, parsedPage));

    setRecentActivityPageDraft(String(nextPage));
    setIsRecentActivityPageEditing(false);

    if (nextPage !== recentActivityPage) {
      setRecentActivityPage(nextPage);
    }
  }

  function cancelRecentActivityPageJump() {
    setRecentActivityPageDraft(String(Math.min(recentActivityPage, recentActivityTotalPages)));
    setIsRecentActivityPageEditing(false);
  }

  function applySolveSubmissionPageJump() {
    const parsedPage = Number.parseInt(solveSubmissionPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? solveSubmissionPage
      : Math.min(solveSubmissionTotalPages, Math.max(1, parsedPage));

    setSolveSubmissionPageDraft(String(nextPage));
    setIsSolveSubmissionPageEditing(false);

    if (nextPage !== solveSubmissionPage) {
      setSolveSubmissionPage(nextPage);
    }
  }

  function cancelSolveSubmissionPageJump() {
    setSolveSubmissionPageDraft(String(Math.min(solveSubmissionPage, solveSubmissionTotalPages)));
    setIsSolveSubmissionPageEditing(false);
  }

  function markProfileAlarmAsRead(alarm: AlarmEntry) {
    if (alarm.read) {
      return;
    }

    setProfileAlarmPageData((currentAlarmPage) => ({
      ...currentAlarmPage,
      unreadCount: Math.max(0, currentAlarmPage.unreadCount - 1),
      alarms: currentAlarmPage.alarms.map((currentAlarm) =>
        currentAlarm.alarmId === alarm.alarmId
          ? {
              ...currentAlarm,
              read: true,
            }
          : currentAlarm,
      ),
    }));

    void markAlarmRead(alarm.alarmId);
  }

  function handleProfileAlarmClick(alarm: AlarmEntry, path?: string, hash?: string) {
    markProfileAlarmAsRead(alarm);

    if (!path || path.trim() === '') {
      return;
    }

    navigate(`${path}${hash ?? ''}`);
  }

  function renderProfileAlarmSentence(alarm: AlarmEntry) {
    const sentence = alarm.sentence.trim();

    if (sentence === '') {
      if (alarm.targetPath && alarm.targetPath.trim() !== '') {
        return (
          <button
            type="button"
            className={`submit-history-link-button profile-alarm-link-button ${alarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}
            onClick={() => handleProfileAlarmClick(alarm, alarm.targetPath, alarm.targetHash)}
          >
            {alarm.message}
          </button>
        );
      }

      return <span className={`profile-alarm-copy ${alarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}>{alarm.message}</span>;
    }

    const parts: Array<string | JSX.Element> = [];
    const tokenPattern = /(\{[^{}]+\}|\([^()]+\))/g;
    let lastIndex = 0;

    for (const tokenMatch of sentence.matchAll(tokenPattern)) {
      const tokenValue = tokenMatch[0];
      const tokenIndex = tokenMatch.index ?? 0;

      if (tokenIndex > lastIndex) {
        parts.push(sentence.slice(lastIndex, tokenIndex));
      }

      const isBraceToken = tokenValue.startsWith('{');
      const tokenKey = tokenValue.slice(1, -1).trim();
      const binding = alarm.bindings[tokenKey];
      const linkText = isBraceToken
        ? (binding?.text && binding.text.trim() !== '' ? binding.text : tokenKey)
        : tokenValue;
      const tooltipText = !isBraceToken && binding?.text ? truncateAlarmHoverText(binding.text) : undefined;
      const tokenPath = binding?.path ?? alarm.targetPath;
      const tokenHash = binding?.hash ?? alarm.targetHash;

      parts.push(
        <button
          key={`${alarm.alarmId}:${tokenKey}:${tokenIndex}`}
          type="button"
          className={`submit-history-link-button profile-alarm-link-button ${alarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}
          title={tooltipText}
          onClick={() => handleProfileAlarmClick(alarm, tokenPath, tokenHash)}
        >
          {linkText}
        </button>,
      );

      lastIndex = tokenIndex + tokenValue.length;
    }

    if (lastIndex < sentence.length) {
      parts.push(sentence.slice(lastIndex));
    }

    return <span className={`profile-alarm-copy ${alarm.alarmType === 'FROM_ADMIN' ? 'is-admin' : ''}`.trim()}>{parts.length > 0 ? parts : alarm.message}</span>;
  }

  function applyProfileAlarmPageJump() {
    const parsedPage = Number.parseInt(profileAlarmPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? profileAlarmPageData.currentPage
      : Math.min(profileAlarmPageData.totalPages, Math.max(1, parsedPage));

    setProfileAlarmPageDraft(String(nextPage));
    setIsProfileAlarmPageEditing(false);

    if (nextPage !== profileAlarmPageData.currentPage) {
      setProfileAlarmPage(nextPage);
    }
  }

  function cancelProfileAlarmPageJump() {
    setProfileAlarmPageDraft(String(profileAlarmPageData.currentPage));
    setIsProfileAlarmPageEditing(false);
  }

  function hasExecutionPlanDetails(history: SubmitHistoryEntry) {
    return getExecutionPlanDetailGroups(history.dbms, history.executionPlanElement).length > 0;
  }

  function updateHeatmapSelection(nextSelection: string[], nextAnchor: string | null) {
    const orderedCellKeys = heatmapCells.map((cell) => cell.key);
    const normalizedSelection = createSortedUniqueDateSelection(nextSelection, orderedCellKeys);
    const normalizedAnchor =
      nextAnchor && normalizedSelection.includes(nextAnchor)
        ? nextAnchor
        : normalizedSelection.length > 0
          ? normalizedSelection[normalizedSelection.length - 1]
          : null;

    setSelectedHeatmapDates(normalizedSelection);
    setHeatmapSelectionAnchor(normalizedAnchor);
    setRecentActivityPage(1);
    setRecentActivityPageDraft('1');
    setIsRecentActivityPageEditing(false);
  }

  function handleHeatmapCellClick(cellKey: string, event: React.MouseEvent<HTMLButtonElement>) {
    const orderedCellKeys = heatmapCells.map((cell) => cell.key);

    if (event.shiftKey && heatmapSelectionAnchor && orderedCellKeys.includes(heatmapSelectionAnchor)) {
      const anchorIndex = orderedCellKeys.indexOf(heatmapSelectionAnchor);
      const targetIndex = orderedCellKeys.indexOf(cellKey);
      const startIndex = Math.min(anchorIndex, targetIndex);
      const endIndex = Math.max(anchorIndex, targetIndex);
      const rangeSelection = orderedCellKeys.slice(startIndex, endIndex + 1);

      if (event.ctrlKey || event.metaKey) {
        updateHeatmapSelection(Array.from(new Set([...selectedHeatmapDates, ...rangeSelection])), cellKey);
        return;
      }

      updateHeatmapSelection(rangeSelection, cellKey);
      return;
    }

    if (event.ctrlKey || event.metaKey) {
      if (selectedHeatmapDates.includes(cellKey)) {
        updateHeatmapSelection(selectedHeatmapDates.filter((date) => date !== cellKey), cellKey);
        return;
      }

      updateHeatmapSelection([...selectedHeatmapDates, cellKey], cellKey);
      return;
    }

    updateHeatmapSelection(selectedHeatmapDates.length === 1 && selectedHeatmapDates[0] === cellKey ? [] : [cellKey], cellKey);
  }

  async function saveProfile() {
    if (!editDraft) {
      return;
    }

    const payload: UpdateUserProfilePayload = {
      bio: editDraft.bio,
      links: normalizeLinksForSave(editDraft.links),
      defaultDbms: editDraft.defaultDbms,
      sqlPublic: editDraft.sqlPublic,
      executionPercentilePublic: editDraft.executionPercentilePublic,
      solvedRecordsPublic: editDraft.solvedRecordsPublic,
      solvedProblemCountPublic: editDraft.solvedProblemCountPublic,
    };

    try {
      const updatedProfile = await updateMyProfile(payload);
      setProfileSummary(updatedProfile);
      setEditDraft(createEditDraft(updatedProfile));
      setIsEditOpen(false);
      setFeedback(null);
      showSessionToast('프로필 저장 완료.');
      void syncSession();
    } catch (error) {
      setFeedback({
        tone: 'error',
        message: error instanceof Error ? error.message : '프로필 저장에 실패했다.',
      });
    }
  }

  function renderSummarySection() {
    return (
      <div className="profile-section-stack profile-summary-stack">
        <div className="profile-summary-grid-layout">
          <section className="panel-card profile-summary-card profile-heatmap-card profile-summary-plain-card">
            <div className="profile-section-heading-row profile-heatmap-heading-row">
              <div className="solve-dbms-tab-row profile-heatmap-year-tabs" role="tablist" aria-label="제출 잔디 연도 선택">
                {heatmapYears.map((year) => {
                  const isSelected = year === selectedHeatmapYear;
                  return (
                    <button
                      key={`heatmap-year-${year}`}
                      type="button"
                      className={`solve-dbms-tab profile-heatmap-year-tab ${isSelected ? 'is-selected' : ''}`}
                      role="tab"
                      aria-selected={isSelected}
                      onClick={() => setSelectedHeatmapYear(year)}
                    >
                      {year}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="profile-heatmap-layout" aria-label="프로필 제출 잔디">
              <div className="profile-heatmap-grid">
                {heatmapCells.map((cell) => {
                  const isSelected = selectedHeatmapDates.includes(cell.key);
                  return (
                    <button
                      key={cell.key}
                      type="button"
                      className={`profile-heatmap-cell-anchor ${isSelected ? 'is-selected is-blinking' : ''}`}
                      aria-label={cell.label}
                      onClick={(event) => handleHeatmapCellClick(cell.key, event)}
                    >
                      <span className={`profile-heatmap-cell ${getHeatmapTone(cell.count)}`} />
                      <span className="profile-heatmap-tooltip" role="tooltip">
                        <span className="profile-heatmap-tooltip-title">{cell.key}</span>
                        <span className="profile-heatmap-tooltip-caption">문제 제출 {numberFormatter.format(cell.submissionCount)}건</span>
                        <span className="profile-heatmap-tooltip-caption">커뮤니티 활동 {numberFormatter.format(cell.communityCount)}건</span>
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </section>
        </div>


        <section className="panel-card profile-summary-card profile-recent-card profile-summary-plain-card">
          {filteredRecentActivities.length > 0 ? (
            <>
              <div className="submit-history-table-shell profile-table-shell profile-recent-table-shell">
                <div className="submit-history-table profile-recent-activity-table" role="table" aria-label="프로필 최근 활동">
                  <div className="submit-history-row submit-history-head profile-recent-activity-head" role="row">
                    <div role="columnheader" className="submit-history-head-cell">내용</div>
                    <div role="columnheader" className="submit-history-head-cell profile-recent-activity-head-date">날짜</div>
                  </div>

                  {pagedRecentActivities.map((activity) =>
                    activity.href ? (
                      <button
                        key={activity.id}
                        type="button"
                        className="submit-history-row submit-history-body profile-recent-activity-item"
                        onClick={() => navigate(activity.href!)}
                      >
                        <span className="submit-history-cell profile-recent-activity-title" role="cell" data-label="내용">
                          {activity.label}
                        </span>
                        <span className="submit-history-cell profile-recent-activity-detail" role="cell" data-label="날짜">
                          {activity.detail}
                        </span>
                      </button>
                    ) : (
                      <div key={activity.id} className="submit-history-row submit-history-body profile-recent-activity-item is-static">
                        <span className="submit-history-cell profile-recent-activity-title" role="cell" data-label="내용">
                          {activity.label}
                        </span>
                        <span className="submit-history-cell profile-recent-activity-detail" role="cell" data-label="날짜">
                          {activity.detail}
                        </span>
                      </div>
                    ),
                  )}
                </div>
              </div>

              {recentActivityTotalPages > 1 ? (
                <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="최근 활동 페이지">
                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setRecentActivityPage((currentPage) => Math.max(1, currentPage - 1))}
                    disabled={recentActivityPage === 1}
                  >
                    이전
                  </button>

                  {isRecentActivityPageEditing ? (
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      className="problem-pagination-meta-input"
                      value={recentActivityPageDraft}
                      onChange={(event) => {
                        const nextValue = event.target.value.replace(/\D+/g, '');
                        setRecentActivityPageDraft(nextValue);
                      }}
                      onBlur={applyRecentActivityPageJump}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.preventDefault();
                          applyRecentActivityPageJump();
                        }

                        if (event.key === 'Escape') {
                          event.preventDefault();
                          cancelRecentActivityPageJump();
                        }
                      }}
                      aria-label="최근 활동 페이지 번호"
                      autoFocus
                    />
                  ) : (
                    <button
                      type="button"
                      className="problem-pagination-meta problem-pagination-meta-button"
                      onClick={() => {
                        setRecentActivityPageDraft(String(recentActivityPage));
                        setIsRecentActivityPageEditing(true);
                      }}
                    >
                      {`${recentActivityPage} / ${recentActivityTotalPages}`}
                    </button>
                  )}

                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setRecentActivityPage((currentPage) => Math.min(recentActivityTotalPages, currentPage + 1))}
                    disabled={recentActivityPage >= recentActivityTotalPages}
                  >
                    다음
                  </button>
                </div>
              ) : null}
            </>
          ) : (
            <div className="submit-history-empty-state profile-inline-empty-state">
              {selectedHeatmapDates.length > 0 ? '선택한 날짜의 활동이 아직 없다.' : '최근 활동이 아직 없다.'}
            </div>
          )}
        </section>
      </div>
    );
  }

  function renderSolveSection() {
    if (!showSolvedProblemSection && !showSubmissionSection) {
      return <div className="submit-history-empty-state">공개된 문제 풀이 정보가 없다.</div>;
    }

    return (
      <div className="profile-section-stack">
        {showSolvedProblemSection ? (
          <section className="panel-card profile-summary-card profile-summary-plain-card profile-solve-chip-card">
            <div className="profile-solve-count-row">
              <span className="profile-solve-count-label">맞은 문제 : {numberFormatter.format(solvedProblemIds.length)}개</span>
            </div>

            {solvedProblemIds.length > 0 ? (
              <div className="profile-solved-chip-list">
                {solvedProblemIds.map((problemId) => (
                  <button
                    key={`solved-${problemId}`}
                    type="button"
                    className="profile-solved-chip"
                    onClick={() => navigate(`/problems/${problemId}`)}
                  >
                    {problemId}
                  </button>
                ))}
              </div>
            ) : (
              <div className="submit-history-empty-state profile-inline-empty-state">아직 해결한 문제가 없다.</div>
            )}

            <div className="profile-solve-count-row profile-solve-count-row-secondary">
              <span className="profile-solve-count-label">틀린 문제 : {numberFormatter.format(failedProblemIds.length)}개</span>
            </div>

            {failedProblemIds.length > 0 ? (
              <div className="profile-solved-chip-list">
                {failedProblemIds.map((problemId) => (
                  <button
                    key={`failed-${problemId}`}
                    type="button"
                    className="profile-solved-chip"
                    onClick={() => navigate(`/problems/${problemId}`)}
                  >
                    {problemId}
                  </button>
                ))}
              </div>
            ) : (
              <div className="submit-history-empty-state profile-inline-empty-state">아직 틀린 문제가 없다.</div>
            )}
          </section>
        ) : null}

        <section className="panel-card profile-summary-card profile-summary-plain-card profile-solve-submit-section">
          {!showSubmissionSection ? (
            <div className="submit-history-empty-state profile-inline-empty-state">공개된 제출 기록이 없다.</div>
          ) : recentSubmissions.length === 0 ? (
            <div className="submit-history-empty-state profile-inline-empty-state">표시할 제출 기록이 없다.</div>
          ) : (
            <>
              <div className="submit-history-table-shell profile-table-shell">
                <div className="submit-history-table profile-solve-submit-table" role="table" aria-label="프로필 문제 제출 목록">
                  <div className="submit-history-row submit-history-head" role="row">
                    <div role="columnheader" className="submit-history-head-cell">제출번호</div>
                    <div role="columnheader" className="submit-history-head-cell">DBMS</div>
                    <div role="columnheader" className="submit-history-head-cell">문제 번호</div>
                    <div role="columnheader" className="submit-history-head-cell">제출 결과</div>
                    <div role="columnheader" className="submit-history-head-cell">Cost</div>
                    <div role="columnheader" className="submit-history-head-cell">제출 시각</div>
                    <div role="columnheader" className="submit-history-head-cell">실행계획요소</div>
                  </div>

                  {pagedSolveSubmissions.map((history) => (
                    <article key={history.submitId} className="submit-history-row submit-history-body" role="row">
                      <span className="submit-history-cell" role="cell" data-label="제출번호">{history.submitId}</span>
                      <span className="submit-history-cell" role="cell" data-label="DBMS">{formatDbmsLabel(history.dbms)}</span>
                      <span className="submit-history-cell" role="cell" data-label="문제 번호">
                        <button type="button" className="submit-history-link-button" onClick={() => navigate(`/problems/${history.problemId}`)}>
                          {history.problemId}
                        </button>
                      </span>
                      <span className="submit-history-cell" role="cell" data-label="제출 결과">
                        <button
                          type="button"
                          className={`submit-history-status-text ${history.success ? 'is-success' : 'is-fail'}`}
                          onClick={() => setSolveModalState({ type: 'sql', history })}
                        >
                          {history.success ? '정답' : '오답'}
                        </button>
                      </span>
                      <span className="submit-history-cell" role="cell" data-label="Cost">
                        {history.success || history.cost > 0 ? formatCost(history.cost) : '-'}
                      </span>
                      <span className="submit-history-cell" role="cell" data-label="제출 시각">{formatDateTime(history.submittedAt)}</span>
                      <span className="submit-history-cell submit-history-cell-plan" role="cell" data-label="실행계획요소">
                        {hasExecutionPlanDetails(history) ? (
                          <button
                            type="button"
                            className="submit-history-detail-button"
                            aria-label={`${getPlanElementButtonLabel(history.dbms, history.executionPlanElement)} 자세히 보기`}
                            title={getPlanElementButtonLabel(history.dbms, history.executionPlanElement)}
                            onClick={() => setSolveModalState({ type: 'plan', history })}
                          >
                            ↗
                          </button>
                        ) : '-'}
                      </span>
                    </article>
                  ))}
                </div>
              </div>

              {solveSubmissionTotalPages > 1 ? (
                <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="문제 제출 목록 페이지">
                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setSolveSubmissionPage((currentPage) => Math.max(1, currentPage - 1))}
                    disabled={solveSubmissionPage === 1}
                  >
                    이전
                  </button>

                  {isSolveSubmissionPageEditing ? (
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      className="problem-pagination-meta-input"
                      value={solveSubmissionPageDraft}
                      onChange={(event) => {
                        const nextValue = event.target.value.replace(/\D+/g, '');
                        setSolveSubmissionPageDraft(nextValue);
                      }}
                      onBlur={applySolveSubmissionPageJump}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.preventDefault();
                          applySolveSubmissionPageJump();
                        }

                        if (event.key === 'Escape') {
                          event.preventDefault();
                          cancelSolveSubmissionPageJump();
                        }
                      }}
                      aria-label="문제 제출 목록 페이지 번호"
                      autoFocus
                    />
                  ) : (
                    <button
                      type="button"
                      className="problem-pagination-meta problem-pagination-meta-button"
                      onClick={() => {
                        setSolveSubmissionPageDraft(String(solveSubmissionPage));
                        setIsSolveSubmissionPageEditing(true);
                      }}
                    >
                      {`${solveSubmissionPage} / ${solveSubmissionTotalPages}`}
                    </button>
                  )}

                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setSolveSubmissionPage((currentPage) => Math.min(solveSubmissionTotalPages, currentPage + 1))}
                    disabled={solveSubmissionPage >= solveSubmissionTotalPages}
                  >
                    다음
                  </button>
                </div>
              ) : null}
            </>
          )}
        </section>
      </div>
    );
  }

  function renderCommunitySection() {
    return (
      <div className="profile-section-stack">
        <section className="panel-card profile-summary-card">
          <div className="profile-section-heading-row profile-community-heading-row">
            <div>
              <p className="panel-meta">커뮤니티 활동</p>
              <h2 className="profile-section-title">커뮤니티 활동</h2>
            </div>
          </div>

          {communityActivities.length === 0 ? (
            <div className="submit-history-empty-state profile-inline-empty-state">표시할 활동이 없다.</div>
          ) : (
            <div className="submit-history-table-shell profile-table-shell">
              <div className="submit-history-table profile-community-activity-table" role="table" aria-label="프로필 커뮤니티 활동">
                <div className="submit-history-row submit-history-head" role="row">
                  <div role="columnheader" className="submit-history-head-cell">내용</div>
                  <div role="columnheader" className="submit-history-head-cell">날짜</div>
                </div>

                {communityActivities.map((activity) => (
                  <article key={activity.id} className="submit-history-row submit-history-body profile-community-activity-row" role="row">
                    <span className="submit-history-cell profile-community-activity-cell" role="cell" data-label="내용">
                      {activity.kind === 'post' ? (
                        <>
                          {activity.tags && activity.tags.length > 0 ? (
                            <span className="profile-community-activity-tags">
                              {activity.tags.slice(0, 7).map((tag) => (
                                <span key={`${activity.id}-${tag}`} className="profile-community-activity-tag">#{tag}</span>
                              ))}
                            </span>
                          ) : null}
                          {activity.href && activity.title ? (
                            <button type="button" className="submit-history-link-button profile-community-activity-title" onClick={() => navigate(activity.href!)}>
                              {activity.title}
                            </button>
                          ) : null}
                          <span className="profile-community-activity-copy">{buildTextSnippet(activity.excerpt ?? '', 240)}</span>
                        </>
                      ) : null}
                      {activity.kind === 'likedPost' ? (
                        activity.href && activity.title ? (
                          <button type="button" className="submit-history-link-button profile-community-activity-title" onClick={() => navigate(activity.href!)}>
                            {buildTextSnippet(activity.title, 120)}
                          </button>
                        ) : null
                      ) : null}
                      {activity.kind === 'comment' ? (
                        <>
                          {activity.href && activity.title ? (
                            <button type="button" className="submit-history-link-button profile-community-activity-title" onClick={() => navigate(activity.href!)}>
                              {buildTextSnippet(activity.title, 120)}
                            </button>
                          ) : null}
                          <span className="profile-community-activity-copy">{buildTextSnippet(activity.content ?? '', 220)}</span>
                        </>
                      ) : null}
                      {activity.kind === 'likedComment' ? (
                        <span className="profile-community-activity-copy">{buildTextSnippet(activity.content ?? '', 220)}</span>
                      ) : null}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="날짜">{formatDateTime(activity.happenedAt)}</span>
                  </article>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    );
  }

  function renderAlarmSection() {
    return (
      <div className="profile-section-stack">
        <section className="panel-card profile-summary-card">
          {profileAlarmErrorMessage ? (
            <PageLoadFailureState className="submit-history-empty-state profile-inline-empty-state" />
          ) : profileAlarmPageData.alarms.length === 0 && !isProfileAlarmLoading ? (
            <div className="submit-history-empty-state profile-inline-empty-state">표시할 알림이 없다.</div>
          ) : (
            <>
              <div className="submit-history-table-shell profile-table-shell">
                <div className={`submit-history-table profile-alarm-table ${isProfileAlarmLoading ? 'is-loading' : ''}`.trim()} role="table" aria-label="프로필 알림 목록">
                  <div className="submit-history-row submit-history-head" role="row">
                    <div role="columnheader" className="submit-history-head-cell">알림 내용</div>
                    <div role="columnheader" className="submit-history-head-cell">날짜</div>
                  </div>

                  {isProfileAlarmLoading && profileAlarmPageData.alarms.length === 0 ? (
                    profileAlarmLoadingRows.map((rowIndex) => (
                      <div key={`profile-alarm-loading-${rowIndex}`} className="submit-history-row submit-history-body profile-alarm-row" role="row" aria-hidden="true">
                        <span className="submit-history-cell profile-alarm-cell" role="cell"><span className="wave-loading-placeholder is-long" /></span>
                        <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                      </div>
                    ))
                  ) : profileAlarmPageData.alarms.map((alarm) => (
                    <article key={alarm.alarmId} className={`submit-history-row submit-history-body profile-alarm-row ${!alarm.read ? 'is-unread' : ''}`.trim()} role="row">
                      <span className="submit-history-cell profile-alarm-cell" role="cell" data-label="알림 내용">
                        {renderProfileAlarmSentence(alarm)}
                      </span>
                      <span className="submit-history-cell" role="cell" data-label="날짜">{formatDateTime(alarm.createdAt)}</span>
                    </article>
                  ))}
                </div>

                {isProfileAlarmLoading ? (
                  <div className="submit-history-loading-overlay" aria-hidden="true">
                    <span className="page-loading-spinner submit-history-loading-badge" />
                  </div>
                ) : null}
              </div>

              {profileAlarmPageData.totalPages > 1 ? (
                <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="알림 목록 페이지">
                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setProfileAlarmPage((currentPage) => Math.max(1, currentPage - 1))}
                    disabled={profileAlarmPageData.currentPage === 1}
                  >
                    이전
                  </button>

                  {isProfileAlarmPageEditing ? (
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      className="problem-pagination-meta-input"
                      value={profileAlarmPageDraft}
                      onChange={(event) => {
                        const nextValue = event.target.value.replace(/\D+/g, '');
                        setProfileAlarmPageDraft(nextValue);
                      }}
                      onBlur={applyProfileAlarmPageJump}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.preventDefault();
                          applyProfileAlarmPageJump();
                        }

                        if (event.key === 'Escape') {
                          event.preventDefault();
                          cancelProfileAlarmPageJump();
                        }
                      }}
                      aria-label="알림 목록 페이지 번호"
                      autoFocus
                    />
                  ) : (
                    <button
                      type="button"
                      className="problem-pagination-meta problem-pagination-meta-button"
                      onClick={() => {
                        setProfileAlarmPageDraft(String(profileAlarmPageData.currentPage));
                        setIsProfileAlarmPageEditing(true);
                      }}
                    >
                      {`${profileAlarmPageData.currentPage} / ${profileAlarmPageData.totalPages}`}
                    </button>
                  )}

                  <button
                    type="button"
                    className="mini-toggle problem-page-button"
                    onClick={() => setProfileAlarmPage((currentPage) => Math.min(profileAlarmPageData.totalPages, currentPage + 1))}
                    disabled={profileAlarmPageData.currentPage >= profileAlarmPageData.totalPages}
                  >
                    다음
                  </button>
                </div>
              ) : null}
            </>
          )}
        </section>
      </div>
    );
  }


  const solveModalContent =
    solveModalState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <div
            className="submit-history-modal-overlay"
            role="presentation"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) {
                setSolveModalState(null);
              }
            }}
          >
            {solveModalState.type === 'sql' ? (
              <div className="submit-history-modal" role="dialog" aria-modal="true" aria-label="제출 SQL 보기">
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <div className="submit-history-modal-title-row">
                      <strong>제출 결과</strong>
                    </div>
                    <div className="submit-history-modal-meta submit-history-modal-meta-stack">
                      <span className="submit-history-modal-meta-line">{solveModalState.history.submitId}</span>
                      <span className="submit-history-modal-meta-line">{solveModalState.history.handle}</span>
                      <span className="submit-history-modal-meta-line">{solveModalState.history.problemId}</span>
                      <span className="submit-history-modal-meta-line">{getDbmsLabel(solveModalState.history.dbms)}</span>
                      <span
                        className={`submit-history-modal-meta-line submit-history-modal-meta-result ${solveModalState.history.success ? 'is-success' : 'is-fail'}`}
                      >
                        {solveModalState.history.success ? '정답' : '오답'}
                      </span>
                      {solveModalState.history.success || solveModalState.history.cost > 0 ? (
                        <span className="submit-history-modal-meta-line">{formatCost(solveModalState.history.cost)}</span>
                      ) : null}
                      <span className="submit-history-modal-meta-line">{formatSubmittedAt(solveModalState.history.submittedAt)}</span>
                      {hasExecutionPlanDetails(solveModalState.history) ? (
                        <button
                          type="button"
                          className="submit-history-modal-meta-action submit-history-modal-meta-icon"
                          aria-label="실행계획 요소 보기"
                          title="실행계획 요소 보기"
                          onClick={() => setSolveModalState({ type: 'plan', history: solveModalState.history })}
                        >
                          ↗
                        </button>
                      ) : null}
                    </div>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setSolveModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-sql-modal-body">
                  <pre className="submit-history-sql-viewer submit-history-sql-highlight" aria-label="제출 SQL">
                    {highlightedSolveModalSql}
                  </pre>
                </div>
              </div>
            ) : (
              <div className="submit-history-modal submit-history-plan-modal" role="dialog" aria-modal="true" aria-label="실행계획 요소 보기">
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>실행계획 요소</strong>
                    <span>
                      {solveModalState.history.handle} · {getDbmsLabel(solveModalState.history.dbms)} · {buildProblemLabel(solveModalState.history.problemId)}
                    </span>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setSolveModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <div className="submit-history-plan-modal-summary">
                    <span className="submit-history-plan-modal-label">Cost</span>
                    <strong>{formatCost(solveModalState.history.cost)}</strong>
                  </div>

                  {getExecutionPlanDetailGroups(solveModalState.history.dbms, solveModalState.history.executionPlanElement).length > 0 ? (
                    <div className="runtime-subfilter-board runtime-plan-shell-panel submit-history-plan-detail-board">
                      {getExecutionPlanDetailGroups(solveModalState.history.dbms, solveModalState.history.executionPlanElement).map((group) => (
                        <div key={`${group.sectionKey}-${group.sectionLabel}`} className="runtime-subfilter-row">
                          <span className="runtime-subfilter-label">{group.sectionLabel}</span>
                          <div className="runtime-subfilter-options submit-history-plan-detail-options">
                            <div className="runtime-subfilter-chip-grid submit-history-plan-detail-grid">
                              {group.labels.map((label) => (
                                <span key={label} className="runtime-subfilter-option">
                                  <span className="runtime-subfilter-button runtime-subfilter-button-plain runtime-check-button is-selected submit-history-plan-static-item">
                                    <SelectionCheckbox checked />
                                    <span className="runtime-check-label">{label}</span>
                                  </span>
                                </span>
                              ))}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="submit-history-empty-state submit-history-modal-empty-state">
                      감지된 대표 실행계획 요소가 없습니다.
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>,
          document.body,
        );

  return (
    <div className="page-stack profile-page submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card profile-tab-shell">
        <div className="problem-toolbar submit-history-toolbar-stack profile-tab-toolbar">
          <div className="solve-dbms-tab-row profile-handle-tab-row" role="tablist" aria-label="프로필 Handle">
            <button
              type="button"
              className={`solve-dbms-tab ${!isEditOpen && !isAlarmListOpen ? 'is-selected' : ''}`}
              role="tab"
              aria-selected={!isEditOpen && !isAlarmListOpen}
              onClick={() => {
                setIsEditOpen(false);
                navigate(profileBasePath, { replace: true });
              }}
            >
              {profileSummary.handle}
            </button>
            {isOwnProfile ? (
              <button
                type="button"
                className={`solve-dbms-tab ${isAlarmListOpen ? 'is-selected' : ''}`}
                role="tab"
                aria-selected={isAlarmListOpen}
                onClick={() => {
                  setIsEditOpen(false);
                  navigate(`${profileBasePath}?tab=alarms`, { replace: true });
                }}
              >
                알림 목록
              </button>
            ) : null}
            {isOwnProfile ? (
              <button
                type="button"
                className={`solve-dbms-tab ${isEditOpen ? 'is-selected' : ''}`}
                role="tab"
                aria-selected={isEditOpen}
                onClick={() => {
                  setEditDraft(createEditDraft(profileSummary));
                  setFeedback(null);
                  setIsEditOpen(true);
                  navigate(profileBasePath, { replace: true });
                }}
              >
                프로필 수정
              </button>
            ) : null}
          </div>
        </div>
      </section>

      <section className="panel-card profile-hero-panel">
        <div className="profile-hero-layout-next">
          <div className="profile-hero-avatar-shell">
            <div className="profile-hero-avatar">{createFallbackAvatarLabel(profileSummary.handle)}</div>
          </div>

          <div className="profile-hero-copy-next">
            <div className="profile-hero-title-row">
              <h1 className="page-title profile-page-title">{profileSummary.handle}</h1>

              {heroLinks.length > 0 ? (
                <div className="profile-hero-inline-link-list" aria-label="외부 링크">
                  {heroLinks.map((link) => (
                    <a
                      key={link.key}
                      href={link.href}
                      className="profile-hero-inline-link"
                      target="_blank"
                      rel="noreferrer"
                      aria-label={`${link.label} ${link.value}`}
                    >
                      <span className="profile-hero-inline-link-label">{link.label}</span>
                      <span className="profile-hero-inline-link-tooltip" role="tooltip">{link.value}</span>
                    </a>
                  ))}
                </div>
              ) : null}
            </div>

            <p className="profile-hero-bio">{profileSummary.bio || '소개글이 아직 없다.'}</p>
          </div>

        </div>
      </section>

      {feedback ? (
        <section className={`panel-card compact profile-feedback-panel is-${feedback.tone}`}>
          <p className="profile-feedback-message">{feedback.message}</p>
        </section>
      ) : null}

      {isOwnProfile && isEditOpen && editDraft ? (
        <section className="panel-card profile-editor-panel-next">
          <div className="profile-editor-grid-next">
            <label className="field-stack profile-editor-field">
              <span className="field-label">자기소개</span>
              <textarea
                className="text-field profile-editor-textarea"
                value={editDraft.bio}
                onChange={(event) =>
                  updateDraft((draft) => ({
                    ...draft,
                    bio: event.target.value,
                  }))
                }
                placeholder="자기소개를 입력해라."
              />
            </label>

            <div className="profile-editor-side-stack">
              <div className="profile-editor-link-list">
                <div className="panel-heading-row responsive">
                  <p className="field-label">링크</p>
                  <button
                    type="button"
                    className="btn ghost"
                    disabled={editDraft.links.length >= 10}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        links: [...draft.links, { type: '', value: '' }],
                      }))
                    }
                  >
                    링크 추가
                  </button>
                </div>

                {editDraft.links.map((link, index) => (
                  <div key={`profile-link-${index}`} className="profile-editor-link-row">
                    <input
                      className="text-field"
                      value={link.type}
                      onChange={(event) =>
                        updateDraft((draft) => ({
                          ...draft,
                          links: draft.links.map((currentLink, currentIndex) =>
                            currentIndex === index
                              ? {
                                  ...currentLink,
                                  type: event.target.value,
                                }
                              : currentLink,
                          ),
                        }))
                      }
                      placeholder="github"
                    />
                    <input
                      className="text-field"
                      value={link.value}
                      onChange={(event) =>
                        updateDraft((draft) => ({
                          ...draft,
                          links: draft.links.map((currentLink, currentIndex) =>
                            currentIndex === index
                              ? {
                                  ...currentLink,
                                  value: event.target.value,
                                }
                              : currentLink,
                          ),
                        }))
                      }
                      placeholder="github.com/quertimizer"
                    />
                    <button
                      type="button"
                      className="btn ghost"
                      disabled={editDraft.links.length === 1}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          links: draft.links.filter((_, currentIndex) => currentIndex !== index),
                        }))
                      }
                    >
                      삭제
                    </button>
                  </div>
                ))}
              </div>

              <div className="profile-editor-dbms-row">
                <p className="field-label">기본 DBMS</p>
                <div className="solve-dbms-tab-row profile-editor-dbms-tabs" role="tablist" aria-label="기본 DBMS 선택">
                  {dbmsOptions.map((option) => {
                    const isSelected = editDraft.defaultDbms === option.value;
                    return (
                      <button
                        key={option.value}
                        type="button"
                        className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                        role="tab"
                        aria-selected={isSelected}
                        onClick={() =>
                          updateDraft((draft) => ({
                            ...draft,
                            defaultDbms: option.value,
                          }))
                        }
                      >
                        {option.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="auth-actions profile-editor-actions-next">
                <button type="button" className="btn primary" onClick={() => void saveProfile()}>
                  저장
                </button>
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => {
                    setEditDraft(createEditDraft(profileSummary));
                    setIsEditOpen(false);
                  }}
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      {isAlarmListOpen ? (
        renderAlarmSection()
      ) : (
        <section className="panel-card profile-main-shell">
          <div className="profile-main-grid-next">
            <aside className="profile-side-nav">
              {profileSections.map((section) => {
                const isSelected = activeSection === section.id;

                return (
                  <button
                    key={section.id}
                    type="button"
                    className={`profile-side-nav-item ${isSelected ? 'is-selected' : ''}`}
                    onClick={() => setActiveSection(section.id)}
                  >
                    <span className="profile-side-nav-label">
                      <span className="profile-side-nav-icon">{renderProfileSectionIcon(section.id)}</span>
                      <strong>{section.label}</strong>
                    </span>
                  </button>
                );
              })}
            </aside>

            <div className="profile-main-content">
              {activeSection === 'summary' ? renderSummarySection() : null}
              {activeSection === 'solve' ? renderSolveSection() : null}
              {activeSection === 'community' ? renderCommunitySection() : null}
            </div>
          </div>
        </section>
      )}
      {solveModalContent}
    </div>
  );
}
