import { useEffect, useMemo, useRef, useState, useSyncExternalStore, type CSSProperties, type ChangeEvent, type MouseEvent, type ReactNode } from 'react';
import {
  fetchCommunityActivitiesByUser,
  fetchCommunityCommentsByUser,
  fetchCommunityPostsByUser,
  fetchLikedCommentsByUser,
  fetchLikedPostsByUser,
  fetchMyCommunityActivities,
  fetchMyCommunityComments,
  fetchMyCommunityPosts,
  fetchMyLikedComments,
  fetchMyLikedPosts,
  uploadCommunityImage,
  type ProfileCommunityActivityPage,
  type ProfileCommunityComment,
  type ProfileCommunityPost,
} from '../lib/communityApi';
import ImageCropModal from '../components/common/ImageCropModal';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getCommunityPostPath, getLocationSearchSnapshot, getProfilePath, navigate, subscribeLocation } from '../lib/navigation';
import { createCroppedImageFile, type ImageCropAreaPixels } from '../lib/imageCrop';
import {
  fetchMyProfileSummary,
  fetchMySolvedProblems,
  fetchMySubmissionSummary,
  fetchProfileSummary,
  fetchSolvedProblems,
  fetchSubmissionSummary,
  updateMyProfile,
  type UpdateUserProfilePayload,
  type UserProfileLink,
  type UserProfileSolvedProblems,
  type UserProfileSubmissionActivity,
  type UserProfileSubmissionSummary,
  type UserProfileSummary,
} from '../lib/profileApi';
import { showSessionErrorToast, showSessionToast, syncSession, useMockSession } from '../lib/session';
import { fetchAlarms, markAlarmRead, type AlarmEntry, type AlarmPageData, type AlarmSortDirection } from '../lib/alarmApi';
import type { DbmsType } from '../types/domain';
import './SubmitHistoryPage.css';
import './ProfilePage.css';

interface ProfilePageProps {
  handle?: string;
}

interface ProfileEditDraft {
  bio: string;
  profileImageUrl: string;
  backgroundImageUrl: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
  communityActivityPublic: boolean;
}

type ProfileImageCropTarget = 'avatar' | 'background';

interface ProfileImageCropState {
  target: ProfileImageCropTarget;
  sourceUrl: string;
  file: File;
}

interface ProfileHeroCoverSize {
  width: number;
  height: number;
}

type EditableProfileSettingKey = 'defaultDbms' | 'sqlPublic' | 'communityActivityPublic';
type ProfileSaveSection = 'card' | 'bio' | 'links' | EditableProfileSettingKey;

const profileAlarmLoadingRows = Array.from({ length: 5 }, (_, index) => index);
const profileCommunityActivityLoadingRows = Array.from({ length: 5 }, (_, index) => index);

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

type ProfileTopTab = 'profile' | 'alarms';

const numberFormatter = new Intl.NumberFormat('ko-KR');
const PROFILE_ALARM_PAGE_SIZE = 10;
const PROFILE_COMMUNITY_ACTIVITY_PAGE_SIZE = 10;
const PROFILE_IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp';
const SUPPORTED_PROFILE_IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'webp']);
const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];
const emptySolvedProblems: UserProfileSolvedProblems = {
  solvedProblemCount: 0,
  solvedProblemIds: [],
};
const emptyProfileSubmissionSummary: UserProfileSubmissionSummary = {
  attemptedProblemIds: [],
  submissionActivities: [],
};
const emptyProfileAlarmPage: AlarmPageData = {
  currentPage: 1,
  pageSize: PROFILE_ALARM_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  unreadCount: 0,
  alarms: [],
};
const emptyProfileCommunityActivityPage: ProfileCommunityActivityPage = {
  currentPage: 1,
  pageSize: PROFILE_COMMUNITY_ACTIVITY_PAGE_SIZE,
  totalCount: 0,
  totalPages: 1,
  activities: [],
};

function createEditDraft(profile: UserProfileSummary): ProfileEditDraft {
  return {
    bio: profile.bio,
    profileImageUrl: profile.profileImageUrl,
    backgroundImageUrl: profile.backgroundImageUrl,
    links: [...profile.links],
    defaultDbms: profile.defaultDbms,
    sqlPublic: profile.sqlPublic,
    executionPercentilePublic: profile.executionPercentilePublic,
    solvedRecordsPublic: profile.solvedRecordsPublic,
    solvedProblemCountPublic: profile.solvedProblemCountPublic,
    communityActivityPublic: profile.communityActivityPublic,
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

function createHeroLinks(links: UserProfileLink[]) {
  const linkGroups = classifyLinkGroups(links);

  return [
    linkGroups.blog ? { key: 'blog', label: 'Blog', value: linkGroups.blog.value, href: resolveLinkHref(linkGroups.blog) } : null,
    linkGroups.email ? { key: 'email', label: 'Email', value: linkGroups.email.value, href: resolveLinkHref(linkGroups.email) } : null,
    linkGroups.github ? { key: 'github', label: 'GitHub', value: linkGroups.github.value, href: resolveLinkHref(linkGroups.github) } : null,
    ...linkGroups.extras.map((link, index) => ({
      key: `extra-${index}-${link.type}-${link.value}`,
      label: link.type,
      value: link.value,
      href: resolveLinkHref(link),
    })),
  ].filter((link): link is { key: string; label: string; value: string; href: string } => link != null);
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

function SortAscendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.5v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M5.2 5.25 8 2.5l2.8 2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortDescendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.6v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="m5.2 10.75 2.8 2.75 2.8-2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.5 4.7h9" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" />
      <path d="M6.4 3.1h3.2" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" />
      <path d="M5 6.2v6.1c0 .55.38.9.92.9h4.16c.54 0 .92-.35.92-.9V6.2" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 7.55v3.7M9 7.55v3.7" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" />
    </svg>
  );
}

function EditIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.25 12.75h2.1l6-6a1.15 1.15 0 0 0 0-1.62l-.48-.48a1.15 1.15 0 0 0-1.62 0l-6 6v2.1Z" stroke="currentColor" strokeWidth="1.35" strokeLinejoin="round" />
      <path d="m8.55 4.95 2.5 2.5" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m3.3 8.45 3 3L12.7 5.2" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M4.1 4.1 11.9 11.9M11.9 4.1 4.1 11.9" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  );
}

function PlusIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 3.1v9.8M3.1 8h9.8" stroke="currentColor" strokeWidth="1.45" strokeLinecap="round" />
    </svg>
  );
}

function ProblemListIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <rect x="2.2" y="2.35" width="11.6" height="11.2" rx="2.1" stroke="currentColor" strokeWidth="1.35" />
      <path d="M5.15 5.35h5.8M5.15 8h5.8M5.15 10.65h3.6" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" />
      <path d="m11.15 10.85 1.1 1.1 1.8-2.1" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
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

function createProfileHeroBackgroundStyle(backgroundImageUrl: string): CSSProperties | undefined {
  if (!backgroundImageUrl) {
    return undefined;
  }

  return {
    backgroundImage: `url("${backgroundImageUrl}")`,
  };
}

function isSupportedProfileImageFile(file: File) {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  return SUPPORTED_PROFILE_IMAGE_EXTENSIONS.has(extension) && (file.type === '' || file.type.startsWith('image/'));
}

function createHeroLinkNodes(links: Array<{ key: string; label: string; value: string; href: string }>) {
  return links.flatMap((link, index) => {
    const nodes: ReactNode[] = [
      (
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
      ),
    ];

    if (index < links.length - 1) {
      nodes.push(
        <span key={`${link.key}-separator`} className="profile-hero-inline-link-separator" aria-hidden="true">
          •
        </span>,
      );
    }

    return nodes;
  });
}

function createHeatmapCells(year: number, submissionActivities: UserProfileSubmissionActivity[], communityDates: string[]) {
  const submissionCountByDate = new Map<string, number>();
  const communityCountByDate = new Map<string, number>();

  submissionActivities.forEach((activity) => {
    const date = new Date(activity.date);

    if (Number.isNaN(date.getTime()) || date.getFullYear() != year) {
      return;
    }

    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    submissionCountByDate.set(key, (submissionCountByDate.get(key) ?? 0) + activity.count);
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

function createHeatmapYears(signupAt: string) {
  const currentYear = new Date().getFullYear();
  const signupDate = new Date(signupAt);
  const firstYear = Number.isNaN(signupDate.getTime()) ? currentYear : Math.min(currentYear, signupDate.getFullYear());

  return Array.from({ length: currentYear - firstYear + 1 }, (_, index) => currentYear - index);
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

      <section className="panel-card profile-loading-panel" aria-live="polite" aria-label="프로필 로딩 중">
        <span className="profile-loading-spinner" aria-hidden="true" />
      </section>
    </div>
  );
}

export default function ProfilePage({ handle: profileHandle }: ProfilePageProps) {
  const { isAuthenticated, isReady, handle: currentHandle } = useMockSession();
  const profileHeroCoverRef = useRef<HTMLDivElement | null>(null);
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const [profileSummary, setProfileSummary] = useState<UserProfileSummary | null>(null);
  const [lastViewedHandle, setLastViewedHandle] = useState<string | null>(profileHandle ?? null);
  const [solvedProblems, setSolvedProblems] = useState<UserProfileSolvedProblems>(emptySolvedProblems);
  const [authoredPosts, setAuthoredPosts] = useState<ProfileCommunityPost[]>([]);
  const [likedPosts, setLikedPosts] = useState<ProfileCommunityPost[]>([]);
  const [communityComments, setCommunityComments] = useState<ProfileCommunityComment[]>([]);
  const [likedComments, setLikedComments] = useState<ProfileCommunityComment[]>([]);
  const [profileSubmissionSummary, setProfileSubmissionSummary] = useState<UserProfileSubmissionSummary>(emptyProfileSubmissionSummary);
  const [profileCommunityActivityPageData, setProfileCommunityActivityPageData] = useState<ProfileCommunityActivityPage>(emptyProfileCommunityActivityPage);
  const [isProfileCommunityActivityLoading, setIsProfileCommunityActivityLoading] = useState(false);
  const [profileCommunityActivityErrorMessage, setProfileCommunityActivityErrorMessage] = useState<string | null>(null);
  const [profileCommunityActivityPage, setProfileCommunityActivityPage] = useState(1);
  const [profileCommunityActivityPageDraft, setProfileCommunityActivityPageDraft] = useState('1');
  const [isProfileCommunityActivityPageEditing, setIsProfileCommunityActivityPageEditing] = useState(false);
  const [profileAlarmPageData, setProfileAlarmPageData] = useState<AlarmPageData>(emptyProfileAlarmPage);
  const [isProfileAlarmLoading, setIsProfileAlarmLoading] = useState(false);
  const [profileAlarmErrorMessage, setProfileAlarmErrorMessage] = useState<string | null>(null);
  const [profileAlarmPage, setProfileAlarmPage] = useState(1);
  const [profileAlarmPageDraft, setProfileAlarmPageDraft] = useState('1');
  const [isProfileAlarmPageEditing, setIsProfileAlarmPageEditing] = useState(false);
  const [profileAlarmSort, setProfileAlarmSort] = useState<AlarmSortDirection>('desc');
  const [profileReloadKey, setProfileReloadKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [, setErrorMessage] = useState<string | null>(null);
  const [selectedHeatmapYear, setSelectedHeatmapYear] = useState(new Date().getFullYear());
  const [selectedHeatmapDates, setSelectedHeatmapDates] = useState<string[]>([]);
  const [heatmapSelectionAnchor, setHeatmapSelectionAnchor] = useState<string | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editDraft, setEditDraft] = useState<ProfileEditDraft | null>(null);
  const [isProfileImageUploading, setIsProfileImageUploading] = useState(false);
  const [isProfileBackgroundUploading, setIsProfileBackgroundUploading] = useState(false);
  const [imageCropState, setImageCropState] = useState<ProfileImageCropState | null>(null);
  const [isImageCropApplying, setIsImageCropApplying] = useState(false);
  const [profileHeroCoverSize, setProfileHeroCoverSize] = useState<ProfileHeroCoverSize>({ width: 0, height: 0 });
  const [isProfileCardEditing, setIsProfileCardEditing] = useState(false);
  const [profileCardSnapshot, setProfileCardSnapshot] = useState<Pick<ProfileEditDraft, 'profileImageUrl' | 'backgroundImageUrl'> | null>(null);
  const [isBioEditing, setIsBioEditing] = useState(false);
  const [bioSnapshot, setBioSnapshot] = useState<string | null>(null);
  const [editingSettingKey, setEditingSettingKey] = useState<EditableProfileSettingKey | null>(null);
  const [editingSettingSnapshot, setEditingSettingSnapshot] = useState<DbmsType | boolean | null>(null);
  const [savingSection, setSavingSection] = useState<ProfileSaveSection | null>(null);
  const [isLinkEditingMode, setIsLinkEditingMode] = useState(false);
  const [, setEditingLinkIndexes] = useState<number[]>([]);
  const [editingLinkSnapshots, setEditingLinkSnapshots] = useState<Record<number, UserProfileLink | null>>({});
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
  const isProfileEditorBusy = savingSection != null;

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!resolvedProfileId) {
      setProfileSummary(null);
      setSolvedProblems(emptySolvedProblems);
      setAuthoredPosts([]);
      setLikedPosts([]);
      setCommunityComments([]);
      setLikedComments([]);
      setProfileSubmissionSummary(emptyProfileSubmissionSummary);
      setProfileCommunityActivityPageData(emptyProfileCommunityActivityPage);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    let cancelled = false;

    setIsLoading(true);
    setErrorMessage(null);
    setIsEditOpen(false);
    setIsProfileCardEditing(false);
    setProfileCardSnapshot(null);
    setIsBioEditing(false);
    setBioSnapshot(null);
    setEditingSettingKey(null);
    setEditingSettingSnapshot(null);
    setSavingSection(null);
    setIsLinkEditingMode(false);
    setEditingLinkIndexes([]);
    setEditingLinkSnapshots({});

    const profileSummaryRequest = isOwnProfile ? fetchMyProfileSummary() : fetchProfileSummary(resolvedProfileId);
    const solvedProblemsRequest = isOwnProfile ? fetchMySolvedProblems() : fetchSolvedProblems(resolvedProfileId);
    const postsRequest = isOwnProfile ? fetchMyCommunityPosts() : fetchCommunityPostsByUser(resolvedProfileId);
    const likedPostsRequest = isOwnProfile ? fetchMyLikedPosts() : fetchLikedPostsByUser(resolvedProfileId);
    const commentsRequest = isOwnProfile ? fetchMyCommunityComments() : fetchCommunityCommentsByUser(resolvedProfileId);
    const likedCommentsRequest = isOwnProfile ? fetchMyLikedComments() : fetchLikedCommentsByUser(resolvedProfileId);
    const submissionSummaryRequest = isOwnProfile ? fetchMySubmissionSummary() : fetchSubmissionSummary(resolvedProfileId);

    Promise.allSettled([
      profileSummaryRequest,
      solvedProblemsRequest,
      postsRequest,
      likedPostsRequest,
      commentsRequest,
      likedCommentsRequest,
      submissionSummaryRequest,
    ]).then((results) => {
      if (cancelled) {
        return;
      }

      const [
        summaryResult,
        solvedProblemsResult,
        postsResult,
        likedPostsResult,
        commentsResult,
        likedCommentsResult,
        submissionSummaryResult,
      ] = results;

      if (summaryResult.status === 'rejected') {
        setProfileSummary(null);
        setEditDraft(null);
        setIsProfileCardEditing(false);
        setProfileCardSnapshot(null);
        setIsBioEditing(false);
        setBioSnapshot(null);
        setEditingSettingKey(null);
        setEditingSettingSnapshot(null);
        setSavingSection(null);
        setIsLinkEditingMode(false);
        setEditingLinkIndexes([]);
        setEditingLinkSnapshots({});
        setErrorMessage(summaryResult.reason instanceof Error ? summaryResult.reason.message : '프로필을 불러오지 못했다.');
        setSolvedProblems(emptySolvedProblems);
        setAuthoredPosts([]);
        setLikedPosts([]);
        setCommunityComments([]);
        setLikedComments([]);
        setProfileSubmissionSummary(emptyProfileSubmissionSummary);
        setProfileCommunityActivityPageData(emptyProfileCommunityActivityPage);
        setIsLoading(false);
        return;
      }

      setProfileSummary(summaryResult.value);
      setLastViewedHandle(summaryResult.value.handle);
      setEditDraft(createEditDraft(summaryResult.value));
      setIsProfileCardEditing(false);
      setProfileCardSnapshot(null);
      setIsBioEditing(false);
      setBioSnapshot(null);
      setEditingSettingKey(null);
      setEditingSettingSnapshot(null);
      setSavingSection(null);
      setIsLinkEditingMode(false);
      setEditingLinkIndexes([]);
      setEditingLinkSnapshots({});
      setSolvedProblems(solvedProblemsResult.status === 'fulfilled' ? solvedProblemsResult.value : emptySolvedProblems);
      setAuthoredPosts(postsResult.status === 'fulfilled' ? postsResult.value : []);
      setLikedPosts(likedPostsResult.status === 'fulfilled' ? likedPostsResult.value : []);
      setCommunityComments(commentsResult.status === 'fulfilled' ? commentsResult.value : []);
      setLikedComments(likedCommentsResult.status === 'fulfilled' ? likedCommentsResult.value : []);
      setProfileSubmissionSummary(submissionSummaryResult.status === 'fulfilled' ? submissionSummaryResult.value : emptyProfileSubmissionSummary);
      setIsLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [isOwnProfile, isReady, profileReloadKey, resolvedProfileId]);

  const heroLinks = useMemo(() => createHeroLinks(profileSummary?.links ?? []), [profileSummary?.links]);
  const editingHeroLinks = useMemo(() => createHeroLinks(editDraft?.links ?? []), [editDraft?.links]);
  const showSolvedProblemSection = isOwnProfile || profileSummary?.solvedProblemCountPublic === true;
  const showSubmissionSection = true;
  const showCommunityActivitySection = isOwnProfile || profileSummary?.communityActivityPublic === true;
  const heatmapCells = useMemo(
    () =>
      createHeatmapCells(
        selectedHeatmapYear,
        profileSubmissionSummary.submissionActivities,
        [
          ...authoredPosts.map((post) => post.createdAt),
          ...likedPosts.map((post) => post.createdAt),
          ...communityComments.map((comment) => comment.createdAt),
          ...likedComments.map((comment) => comment.createdAt),
        ],
      ),
    [authoredPosts, communityComments, likedComments, likedPosts, profileSubmissionSummary.submissionActivities, selectedHeatmapYear],
  );
  const heatmapYears = useMemo(() => createHeatmapYears(profileSummary?.signupAt ?? ''), [profileSummary?.signupAt]);
  const solvedProblemIds = useMemo(() => [...solvedProblems.solvedProblemIds], [solvedProblems.solvedProblemIds]);
  const attemptedProblemIds = useMemo(() => [...profileSubmissionSummary.attemptedProblemIds], [profileSubmissionSummary.attemptedProblemIds]);
  const profileBackgroundCropAspect = useMemo(
    () =>
      profileHeroCoverSize.width > 0 && profileHeroCoverSize.height > 0
        ? profileHeroCoverSize.width / profileHeroCoverSize.height
        : 2048 / 292,
    [profileHeroCoverSize.height, profileHeroCoverSize.width],
  );
  const isLinkBatchEditing = isLinkEditingMode;

  useEffect(() => {
    setSelectedHeatmapYear(new Date().getFullYear());
    setSelectedHeatmapDates([]);
    setHeatmapSelectionAnchor(null);
    setProfileCommunityActivityPage(1);
    setProfileCommunityActivityPageDraft('1');
    setIsProfileCommunityActivityPageEditing(false);
    setProfileCommunityActivityPageData(emptyProfileCommunityActivityPage);
    setProfileCommunityActivityErrorMessage(null);
    setProfileAlarmPage(1);
    setProfileAlarmPageDraft('1');
    setIsProfileAlarmPageEditing(false);
    setProfileAlarmPageData(emptyProfileAlarmPage);
    setProfileAlarmErrorMessage(null);
    setEditingSettingKey(null);
    setEditingSettingSnapshot(null);
    setSavingSection(null);
    setIsLinkEditingMode(false);
  }, [resolvedProfileId]);

  useEffect(() => {
    if (heatmapYears.length === 0) {
      return;
    }

    if (!heatmapYears.includes(selectedHeatmapYear)) {
      setSelectedHeatmapYear(heatmapYears[0]);
    }
  }, [heatmapYears, selectedHeatmapYear]);

  useEffect(() => {
    setSelectedHeatmapDates([]);
    setHeatmapSelectionAnchor(null);
  }, [selectedHeatmapYear]);

  useEffect(() => {
    return () => {
      if (imageCropState) {
        URL.revokeObjectURL(imageCropState.sourceUrl);
      }
    };
  }, [imageCropState]);

  useEffect(() => {
    const targetElement = profileHeroCoverRef.current;
    if (!targetElement) {
      return;
    }
    const observedElement = targetElement;

    function syncProfileHeroCoverSize() {
      setProfileHeroCoverSize({
        width: observedElement.clientWidth,
        height: observedElement.clientHeight,
      });
    }

    syncProfileHeroCoverSize();

    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', syncProfileHeroCoverSize);
      return () => window.removeEventListener('resize', syncProfileHeroCoverSize);
    }

    const resizeObserver = new ResizeObserver(() => {
      syncProfileHeroCoverSize();
    });

    resizeObserver.observe(observedElement);
    return () => resizeObserver.disconnect();
  }, [isEditOpen, profileSummary?.handle]);

  useEffect(() => {
    if (!resolvedProfileId) {
      return;
    }

    let cancelled = false;
    setIsProfileCommunityActivityLoading(true);
    setProfileCommunityActivityErrorMessage(null);

    const request = isOwnProfile
      ? fetchMyCommunityActivities(profileCommunityActivityPage, PROFILE_COMMUNITY_ACTIVITY_PAGE_SIZE)
      : fetchCommunityActivitiesByUser(resolvedProfileId, profileCommunityActivityPage, PROFILE_COMMUNITY_ACTIVITY_PAGE_SIZE);

    request
      .then((nextCommunityActivityPage) => {
        if (cancelled) {
          return;
        }

        setProfileCommunityActivityPageData(nextCommunityActivityPage);
        if (nextCommunityActivityPage.currentPage !== profileCommunityActivityPage) {
          setProfileCommunityActivityPage(nextCommunityActivityPage.currentPage);
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setProfileCommunityActivityErrorMessage(error instanceof Error ? error.message : '커뮤니티 활동을 불러오지 못했다.');
      })
      .finally(() => {
        if (!cancelled) {
          setIsProfileCommunityActivityLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isOwnProfile, profileCommunityActivityPage, resolvedProfileId]);

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

    fetchAlarms(profileAlarmPage, PROFILE_ALARM_PAGE_SIZE, profileAlarmSort)
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
  }, [isAlarmListOpen, isOwnProfile, profileAlarmPage, profileAlarmSort]);

  useEffect(() => {
    if (!isProfileCommunityActivityPageEditing) {
      setProfileCommunityActivityPageDraft(String(profileCommunityActivityPage));
    }
  }, [isProfileCommunityActivityPageEditing, profileCommunityActivityPage]);

  useEffect(() => {
    if (profileCommunityActivityPage > profileCommunityActivityPageData.totalPages) {
      setProfileCommunityActivityPage(profileCommunityActivityPageData.totalPages);
    }
  }, [profileCommunityActivityPage, profileCommunityActivityPageData.totalPages]);

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

        <section className="panel-card profile-empty-panel profile-load-failure-panel">
          <p className="profile-load-failure-text">잠시 후 다시 시도해주세요</p>
        </section>
      </div>
    );
  }

  function updateDraft(updater: (draft: ProfileEditDraft) => ProfileEditDraft) {
    setEditDraft((currentDraft) => (currentDraft ? updater(currentDraft) : currentDraft));
  }

  function openProfileCardEditing() {
    if (!editDraft || isProfileEditorBusy) {
      return;
    }

    cancelActiveProfileEditing();
    setProfileCardSnapshot({
      profileImageUrl: editDraft.profileImageUrl,
      backgroundImageUrl: editDraft.backgroundImageUrl,
    });
    setIsProfileCardEditing(true);
  }

  function refreshProfileView() {
    setIsLoading(true);
    setErrorMessage(null);
    setProfileReloadKey((currentKey) => currentKey + 1);
  }

  function startBioEditing() {
    if (!editDraft || isProfileEditorBusy) {
      return;
    }

    cancelActiveProfileEditing();
    setBioSnapshot(editDraft.bio);
    setIsBioEditing(true);
  }

  async function completeBioEditing() {
    if (await saveProfileSection('bio', '소개 저장 완료.')) {
      setBioSnapshot(null);
      setIsBioEditing(false);
    }
  }

  function cancelBioEditing() {
    if (bioSnapshot != null) {
      updateDraft((draft) => ({
        ...draft,
        bio: bioSnapshot,
      }));
    }

    setBioSnapshot(null);
    setIsBioEditing(false);
  }

  async function completeProfileCardEditing() {
    if (await saveProfileSection('card', '프로필 카드 저장 완료.')) {
      setProfileCardSnapshot(null);
      setIsProfileCardEditing(false);
    }
  }

  function cancelProfileCardEditing() {
    if (profileCardSnapshot) {
      updateDraft((draft) => ({
        ...draft,
        profileImageUrl: profileCardSnapshot.profileImageUrl,
        backgroundImageUrl: profileCardSnapshot.backgroundImageUrl,
      }));
    }

    setProfileCardSnapshot(null);
    setIsProfileCardEditing(false);
  }

  function startAllLinksEditing() {
    if (!editDraft || isProfileEditorBusy) {
      return;
    }

    cancelActiveProfileEditing();
    setIsLinkEditingMode(true);
    setEditingLinkIndexes(editDraft.links.map((_, index) => index));
    setEditingLinkSnapshots(
      editDraft.links.reduce<Record<number, UserProfileLink | null>>((nextSnapshots, link, index) => {
        nextSnapshots[index] = { ...link };
        return nextSnapshots;
      }, {}),
    );
  }

  async function completeAllLinksEditing() {
    if (await saveProfileSection('links', '링크 저장 완료.')) {
      setIsLinkEditingMode(false);
      setEditingLinkIndexes([]);
      setEditingLinkSnapshots({});
    }
  }

  function cancelAllLinksEditing() {
    if (!editDraft) {
      return;
    }

    updateDraft((draft) => ({
      ...draft,
      links: draft.links.reduce<UserProfileLink[]>((nextLinks, currentLink, currentIndex) => {
        const snapshot = editingLinkSnapshots[currentIndex];

        if (snapshot === null) {
          return nextLinks;
        }

        if (snapshot) {
          nextLinks.push(snapshot);
          return nextLinks;
        }

        nextLinks.push(currentLink);

        return nextLinks;
      }, []),
    }));
    setIsLinkEditingMode(false);
    setEditingLinkIndexes([]);
    setEditingLinkSnapshots({});
  }

  function startSettingEditing(key: EditableProfileSettingKey) {
    if (!editDraft || isProfileEditorBusy) {
      return;
    }

    cancelActiveProfileEditing();
    setEditingSettingKey(key);
    setEditingSettingSnapshot(editDraft[key]);
  }

  async function completeSettingEditing(key: EditableProfileSettingKey) {
    if (editingSettingKey !== key) {
      return;
    }

    const successMessages: Record<EditableProfileSettingKey, string> = {
      defaultDbms: '기본 DBMS 저장 완료.',
      sqlPublic: 'SQL 공개 설정 저장 완료.',
      communityActivityPublic: '커뮤니티 활동 공개 설정 저장 완료.',
    };

    if (await saveProfileSection(key, successMessages[key])) {
      setEditingSettingKey(null);
      setEditingSettingSnapshot(null);
    }
  }

  function cancelSettingEditing(key: EditableProfileSettingKey) {
    if (!editDraft || editingSettingKey !== key) {
      return;
    }

    if (editingSettingSnapshot != null) {
      updateDraft((draft) => ({
        ...draft,
        [key]: editingSettingSnapshot,
      }));
    }

    setEditingSettingKey(null);
    setEditingSettingSnapshot(null);
  }

  function cancelActiveProfileEditing() {
    if (isProfileCardEditing) {
      cancelProfileCardEditing();
    }

    if (isBioEditing) {
      cancelBioEditing();
    }

    if (isLinkEditingMode) {
      cancelAllLinksEditing();
    }

    if (editingSettingKey != null) {
      cancelSettingEditing(editingSettingKey);
    }
  }

  function updateLinkDraft(index: number, key: 'type' | 'value', value: string) {
    updateDraft((draft) => ({
      ...draft,
      links: draft.links.map((currentLink, currentIndex) =>
        currentIndex === index
          ? {
              ...currentLink,
              [key]: value,
            }
          : currentLink,
      ),
    }));
  }

  function removeLinkDraft(index: number) {
    updateDraft((draft) => ({
      ...draft,
      links: draft.links.filter((_, currentIndex) => currentIndex !== index),
    }));
    setEditingLinkIndexes((currentIndexes) =>
      currentIndexes
        .filter((currentIndex) => currentIndex !== index)
        .map((currentIndex) => (currentIndex > index ? currentIndex - 1 : currentIndex)),
    );
    setEditingLinkSnapshots((currentSnapshots) =>
      Object.entries(currentSnapshots).reduce<Record<number, UserProfileLink | null>>((nextSnapshots, [currentIndex, snapshot]) => {
        const parsedIndex = Number.parseInt(currentIndex, 10);

        if (parsedIndex === index) {
          return nextSnapshots;
        }

        nextSnapshots[parsedIndex > index ? parsedIndex - 1 : parsedIndex] = snapshot;
        return nextSnapshots;
      }, {}),
    );
  }

  function addLinkDraft() {
    if (!editDraft || !isLinkBatchEditing || editDraft.links.length >= 10) {
      return;
    }

    const nextIndex = editDraft.links.length;
    updateDraft((draft) => ({
      ...draft,
      links: [...draft.links, { type: '', value: '' }],
    }));
    setEditingLinkIndexes((currentIndexes) => [...currentIndexes, nextIndex]);
    setEditingLinkSnapshots((currentSnapshots) => ({
      ...currentSnapshots,
      [nextIndex]: null,
    }));
  }

  function moveProfileCommunityActivityPage(nextPage: number) {
    if (nextPage === profileCommunityActivityPage) {
      return;
    }

    setProfileCommunityActivityErrorMessage(null);
    setIsProfileCommunityActivityLoading(true);
    setProfileCommunityActivityPage(nextPage);
  }

  function applyProfileCommunityActivityPageJump() {
    const parsedPage = Number.parseInt(profileCommunityActivityPageDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? profileCommunityActivityPage
      : Math.min(profileCommunityActivityPageData.totalPages, Math.max(1, parsedPage));

    setProfileCommunityActivityPageDraft(String(nextPage));
    setIsProfileCommunityActivityPageEditing(false);

    if (nextPage !== profileCommunityActivityPage) {
      moveProfileCommunityActivityPage(nextPage);
    }
  }

  function cancelProfileCommunityActivityPageJump() {
    setProfileCommunityActivityPageDraft(String(profileCommunityActivityPage));
    setIsProfileCommunityActivityPageEditing(false);
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

    const parts: ReactNode[] = [];
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
  }

  function handleHeatmapCellClick(cellKey: string, event: MouseEvent<HTMLButtonElement>) {
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

  function openImageCropModal(target: ProfileImageCropTarget, file: File) {
    if (imageCropState) {
      URL.revokeObjectURL(imageCropState.sourceUrl);
    }

    try {
      setImageCropState({
        target,
        file,
        sourceUrl: URL.createObjectURL(file),
      });
    } catch {
      showSessionErrorToast('이미지 파일을 불러오지 못했습니다.');
    }
  }

  function closeImageCropModal() {
    setImageCropState((currentState) => {
      if (currentState) {
        URL.revokeObjectURL(currentState.sourceUrl);
      }

      return null;
    });
    setIsImageCropApplying(false);
  }

  function handleProfileImageSelect(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file) {
      return;
    }

    if (!isSupportedProfileImageFile(file)) {
      showSessionErrorToast('사진 파일만 업로드할 수 있습니다.');
      return;
    }

    openImageCropModal('avatar', file);
  }

  function handleProfileBackgroundImageSelect(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file) {
      return;
    }

    if (!isSupportedProfileImageFile(file)) {
      showSessionErrorToast('사진 파일만 업로드할 수 있습니다.');
      return;
    }

    openImageCropModal('background', file);
  }

  async function applyImageCrop(areaPixels: ImageCropAreaPixels) {
    if (!imageCropState || !editDraft) {
      return;
    }

    const target = imageCropState.target;
    const outputSize = target === 'avatar'
      ? { width: 512, height: 512, fileName: `${profileSummary?.handle ?? 'profile'}-avatar` }
      : {
          width: 2048,
          height: Math.max(1, Math.round(2048 / profileBackgroundCropAspect)),
          fileName: `${profileSummary?.handle ?? 'profile'}-background`,
        };

    try {
      setIsImageCropApplying(true);
      const croppedFile = await createCroppedImageFile({
        file: imageCropState.file,
        cropAreaPixels: areaPixels,
        outputWidth: outputSize.width,
        outputHeight: outputSize.height,
        fileName: outputSize.fileName,
        quality: target === 'background' ? 1 : 0.98,
      });
      const previewUrl = URL.createObjectURL(croppedFile);
      closeImageCropModal();
      await uploadCroppedProfileAsset(target, croppedFile, previewUrl);
    } catch (error) {
      showSessionErrorToast(error instanceof Error ? error.message : '이미지 편집에 실패했다.');
      setIsImageCropApplying(false);
    }
  }

  async function uploadCroppedProfileAsset(target: ProfileImageCropTarget, file: File, previewUrl: string) {
    const draftKey = target === 'avatar' ? 'profileImageUrl' : 'backgroundImageUrl';
    const previousImageUrl = editDraft?.[draftKey] ?? '';

    updateDraft((draft) => ({
      ...draft,
      [draftKey]: previewUrl,
    }));

    try {
      if (target === 'avatar') {
        setIsProfileImageUploading(true);
      } else {
        setIsProfileBackgroundUploading(true);
      }

      const uploadedImage = await uploadCommunityImage(file);
      updateDraft((draft) =>
        draft[draftKey] === previewUrl
          ? {
              ...draft,
              [draftKey]: uploadedImage.imageUrl,
            }
          : draft,
      );
      showSessionToast(target === 'avatar' ? '프로필 사진 업로드 완료.' : '프로필 배경 업로드 완료.');
    } catch (error) {
      updateDraft((draft) =>
        draft[draftKey] === previewUrl
          ? {
              ...draft,
              [draftKey]: previousImageUrl,
            }
          : draft,
      );
      showSessionErrorToast(error instanceof Error ? error.message : target === 'avatar' ? '프로필 사진 업로드에 실패했다.' : '프로필 배경 업로드에 실패했다.');
    } finally {
      URL.revokeObjectURL(previewUrl);

      if (target === 'avatar') {
        setIsProfileImageUploading(false);
      } else {
        setIsProfileBackgroundUploading(false);
      }
    }
  }

  function renderProfileAvatar(imageUrl: string, handle: string, className = '') {
    return (
      <div className={`profile-hero-avatar ${className}`.trim()}>
        {imageUrl ? <img src={imageUrl} alt={`${handle} 프로필 사진`} /> : createFallbackAvatarLabel(handle)}
      </div>
    );
  }

  function createUpdateUserProfilePayload(draft: ProfileEditDraft): UpdateUserProfilePayload {
    return {
      bio: draft.bio,
      profileImageUrl: draft.profileImageUrl,
      backgroundImageUrl: draft.backgroundImageUrl,
      links: normalizeLinksForSave(draft.links),
      defaultDbms: draft.defaultDbms,
      sqlPublic: draft.sqlPublic,
      executionPercentilePublic: draft.executionPercentilePublic,
      solvedRecordsPublic: draft.solvedRecordsPublic,
      solvedProblemCountPublic: draft.solvedProblemCountPublic,
      communityActivityPublic: draft.communityActivityPublic,
    };
  }

  async function saveProfileSection(section: ProfileSaveSection, successMessage: string) {
    if (!editDraft) {
      return false;
    }

    try {
      setSavingSection(section);
      const updatedProfile = await updateMyProfile(createUpdateUserProfilePayload(editDraft));
      setProfileSummary(updatedProfile);
      setEditDraft(createEditDraft(updatedProfile));
      showSessionToast(successMessage);
      void syncSession();
      return true;
    } catch (error) {
      showSessionErrorToast(error instanceof Error ? error.message : '프로필 저장에 실패했다.');
      return false;
    } finally {
      setSavingSection((currentSection) => (currentSection === section ? null : currentSection));
    }
  }

  function renderSummarySection() {
    return (
      <section className="profile-flow-section profile-heatmap-section">
        <div className="profile-section-heading-row profile-heatmap-heading-row">
          <div className="solve-dbms-tab-row profile-heatmap-year-tabs" role="tablist" aria-label="활동 캘린더 연도 선택">
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

        <div className="profile-heatmap-layout" aria-label="프로필 활동 캘린더">
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
    );
  }

  function renderSolveSection() {
    if (!showSolvedProblemSection && !showSubmissionSection) {
      return <div className="submit-history-empty-state">공개된 문제 풀이 정보가 없다.</div>;
    }

    const solvedProblemIdSet = new Set(solvedProblemIds);
    const wrongProblemIds = attemptedProblemIds.filter((problemId) => !solvedProblemIdSet.has(problemId));
    const orderedProblemIds = [
      ...solvedProblemIds.map((problemId) => ({ problemId, status: 'solved' as const })),
      ...wrongProblemIds.map((problemId) => ({ problemId, status: 'wrong' as const })),
    ];

    return (
      <section className="profile-flow-section profile-problem-section">
        <div className="profile-flow-title-row">
          <span className="profile-flow-title-icon is-solve" aria-hidden="true"><ProblemListIcon /></span>
          <h2 className="profile-section-title">문제 풀이 현황</h2>
        </div>

        <div className="profile-problem-content-shell">
          <div className="profile-problem-toolbar is-stat-only">
            <div className="profile-problem-stat-grid is-inline" aria-label="문제 풀이 요약">
              <div className="profile-problem-stat-card">
                <span className="profile-problem-stat-label">시도한 문제</span>
                <strong className="profile-problem-stat-value">{numberFormatter.format(attemptedProblemIds.length)}개</strong>
              </div>
              <div className="profile-problem-stat-card">
                <span className="profile-problem-stat-label">정답</span>
                <strong className="profile-problem-stat-value is-solved">{numberFormatter.format(solvedProblemIds.length)}개</strong>
              </div>
              <div className="profile-problem-stat-card">
                <span className="profile-problem-stat-label">오답</span>
                <strong className="profile-problem-stat-value is-attempted">{numberFormatter.format(wrongProblemIds.length)}개</strong>
              </div>
            </div>
          </div>

          <div className={`profile-problem-panel is-combined ${orderedProblemIds.length === 0 ? 'is-empty' : ''}`.trim()}>
            {orderedProblemIds.length > 0 ? (
              <div className="profile-solved-chip-list profile-problem-chip-list">
                {orderedProblemIds.map((entry) => (
                  <button
                    key={`${entry.status}-${entry.problemId}`}
                    type="button"
                    className={`profile-solved-chip ${entry.status === 'solved' ? 'is-solved' : 'is-attempted'}`.trim()}
                    onClick={() => navigate(`/problems/${entry.problemId}`)}
                  >
                    {entry.problemId}
                  </button>
                ))}
              </div>
            ) : (
              <div className="submit-history-empty-state profile-inline-empty-state">시도한 문제 없음</div>
            )}
          </div>
        </div>
      </section>
    );
  }

  function renderCommunityActivitySentence(activity: ProfileCommunityActivityPage['activities'][number]) {
    const postLink = (
      <button
        type="button"
        className="submit-history-link-button profile-community-activity-title"
        onClick={() => navigate(getCommunityPostPath(activity.postId))}
      >
        {buildTextSnippet(activity.postTitle, 75)}
      </button>
    );
    const commentLink = (
      <button
        type="button"
        className="submit-history-link-button profile-community-activity-word-link"
        onClick={() => navigate(`${getCommunityPostPath(activity.postId)}${activity.commentId ? `#community-comment-${activity.commentId}` : ''}`)}
        aria-label="댓글로 이동"
      >
        댓글
      </button>
    );
    const likeText = <span className="profile-community-activity-plain-word">좋아요</span>;

    if (activity.activityType === 'post') {
      return <>커뮤니티에 {postLink} 을 작성했습니다.</>;
    }

    if (activity.activityType === 'likedPost') {
      return <>{postLink} 에 {likeText}를 남겼습니다.</>;
    }

    if (activity.activityType === 'comment') {
      return <>{postLink} 에 {commentLink}을 남겼습니다.</>;
    }

    return <>{postLink} 의 {commentLink}에 {likeText}를 남겼습니다.</>;
  }

  function renderCommunitySection() {
    return (
      <section className="profile-flow-section profile-community-section">
        <div className="profile-section-heading-row profile-community-heading-row">
          <div className="profile-flow-title-row">
            <span className="profile-flow-title-icon is-community" aria-hidden="true">✎</span>
            <h2 className="profile-section-title">커뮤니티 활동</h2>
          </div>
        </div>

        {!showCommunityActivitySection ? (
          <div className="submit-history-empty-state profile-inline-empty-state">공개된 커뮤니티 활동이 없다.</div>
        ) : profileCommunityActivityErrorMessage ? (
          <PageLoadFailureState className="submit-history-empty-state profile-inline-empty-state" />
        ) : profileCommunityActivityPageData.activities.length === 0 && !isProfileCommunityActivityLoading ? (
          <div className="submit-history-empty-state profile-inline-empty-state">활동 없음</div>
        ) : (
          <>
            <div className={`submit-history-table-shell profile-table-shell ${isProfileCommunityActivityLoading ? 'is-loading' : ''}`.trim()}>
              <div className={`submit-history-table profile-community-activity-table ${isProfileCommunityActivityLoading ? 'is-loading' : ''}`.trim()} role="table" aria-label="프로필 커뮤니티 활동">
                <div className="submit-history-row submit-history-head" role="row">
                  <div role="columnheader" className="submit-history-head-cell">내용</div>
                  <div role="columnheader" className="submit-history-head-cell">날짜</div>
                </div>

                {isProfileCommunityActivityLoading && profileCommunityActivityPageData.activities.length === 0 ? (
                  profileCommunityActivityLoadingRows.map((rowIndex) => (
                    <div key={`profile-community-activity-loading-${rowIndex}`} className="submit-history-row submit-history-body profile-community-activity-row" role="row" aria-hidden="true">
                      <span className="submit-history-cell profile-community-activity-cell" role="cell"><span className="wave-loading-placeholder is-long" /></span>
                      <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                    </div>
                  ))
                ) : profileCommunityActivityPageData.activities.map((activity) => (
                  <article key={`${activity.activityType}-${activity.postId}-${activity.commentId ?? 'post'}-${activity.happenedAt}`} className="submit-history-row submit-history-body profile-community-activity-row" role="row">
                    <span className="submit-history-cell profile-community-activity-cell" role="cell" data-label="내용">
                      <span className="profile-community-activity-copy">{renderCommunityActivitySentence(activity)}</span>
                    </span>
                    <span className="submit-history-cell profile-community-activity-date" role="cell" data-label="날짜">{formatDateTime(activity.happenedAt)}</span>
                  </article>
                ))}
              </div>

              {isProfileCommunityActivityLoading ? (
                <div className="submit-history-loading-overlay" aria-hidden="true">
                  <span className="page-loading-spinner submit-history-loading-badge" />
                </div>
              ) : null}
            </div>

            {profileCommunityActivityPageData.totalPages > 1 ? (
              <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="커뮤니티 활동 페이지">
                <button
                  type="button"
                  className="mini-toggle problem-page-button"
                  onClick={() => moveProfileCommunityActivityPage(Math.max(1, profileCommunityActivityPage - 1))}
                  disabled={profileCommunityActivityPage === 1}
                >
                  이전
                </button>

                {isProfileCommunityActivityPageEditing ? (
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    className="problem-pagination-meta-input"
                    value={profileCommunityActivityPageDraft}
                    onChange={(event) => {
                      const nextValue = event.target.value.replace(/\D+/g, '');
                      setProfileCommunityActivityPageDraft(nextValue);
                    }}
                    onBlur={applyProfileCommunityActivityPageJump}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault();
                        applyProfileCommunityActivityPageJump();
                      }

                      if (event.key === 'Escape') {
                        event.preventDefault();
                        cancelProfileCommunityActivityPageJump();
                      }
                    }}
                    aria-label="커뮤니티 활동 페이지 번호"
                    autoFocus
                  />
                ) : (
                  <button
                    type="button"
                    className="problem-pagination-meta problem-pagination-meta-button"
                    onClick={() => {
                      setProfileCommunityActivityPageDraft(String(profileCommunityActivityPage));
                      setIsProfileCommunityActivityPageEditing(true);
                    }}
                  >
                    {`${profileCommunityActivityPage} / ${profileCommunityActivityPageData.totalPages}`}
                  </button>
                )}

                <button
                  type="button"
                  className="mini-toggle problem-page-button"
                  onClick={() => moveProfileCommunityActivityPage(Math.min(profileCommunityActivityPageData.totalPages, profileCommunityActivityPage + 1))}
                  disabled={profileCommunityActivityPage >= profileCommunityActivityPageData.totalPages}
                >
                  다음
                </button>
              </div>
            ) : null}
          </>
        )}
      </section>
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
              <div className={`submit-history-table-shell profile-table-shell ${isProfileAlarmLoading ? 'is-loading' : ''}`.trim()}>
                <div className={`submit-history-table profile-alarm-table ${isProfileAlarmLoading ? 'is-loading' : ''}`.trim()} role="table" aria-label="프로필 알림 목록">
                  <div className="submit-history-row submit-history-head" role="row">
                    <div role="columnheader" className="submit-history-head-cell">알림 내용</div>
                    <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter profile-alarm-sort-head">
                      <span>날짜</span>
                      <button
                        type="button"
                        className="submit-history-head-filter-trigger submit-history-head-sort-trigger is-active profile-alarm-sort-button"
                        aria-label={`날짜 ${profileAlarmSort === 'desc' ? '오름차순' : '내림차순'} 정렬`}
                        onClick={() => {
                          setProfileAlarmSort((currentSort) => (currentSort === 'desc' ? 'asc' : 'desc'));
                          setProfileAlarmPage(1);
                        }}
                      >
                        {profileAlarmSort === 'desc' ? <SortDescendingIcon /> : <SortAscendingIcon />}
                      </button>
                    </div>
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

  return (
    <>
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
                  setIsProfileCardEditing(false);
                  setProfileCardSnapshot(null);
                  setIsBioEditing(false);
                  setBioSnapshot(null);
                  refreshProfileView();
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
                    setIsProfileCardEditing(false);
                    setProfileCardSnapshot(null);
                    setIsBioEditing(false);
                    setBioSnapshot(null);
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
                    setIsProfileCardEditing(false);
                    setProfileCardSnapshot(null);
                    setIsBioEditing(false);
                    setBioSnapshot(null);
                    setIsLinkEditingMode(false);
                    setEditingLinkIndexes([]);
                    setEditingLinkSnapshots({});
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

        {isOwnProfile && isEditOpen && editDraft ? (
          <section className="profile-editor-panel-next">
            <div className="profile-editor-block is-card">
              <div className="profile-editor-block-heading">
                <p className="field-label">프로필 카드</p>
                <div className="profile-editor-heading-actions">
                  {isProfileCardEditing ? (
                    <>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-save-button"
                        aria-label="프로필 카드 수정 완료"
                        title="완료"
                        disabled={savingSection === 'card'}
                        onClick={() => void completeProfileCardEditing()}
                      >
                        <CheckIcon />
                      </button>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-cancel-button"
                        aria-label="프로필 카드 수정 취소"
                        title="취소"
                        disabled={savingSection === 'card'}
                        onClick={cancelProfileCardEditing}
                      >
                        <CloseIcon />
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn text profile-editor-icon-button"
                      aria-label="프로필 카드 수정"
                      title="수정"
                      disabled={isProfileEditorBusy}
                      onClick={openProfileCardEditing}
                    >
                      <EditIcon />
                    </button>
                  )}
                </div>
              </div>

              <section className={`panel-card profile-hero-panel profile-editor-hero-panel ${isProfileCardEditing ? 'is-editing' : ''}`.trim()}>
              <div ref={profileHeroCoverRef} className={`profile-hero-cover ${editDraft.backgroundImageUrl ? '' : 'is-default-background'}`.trim()}>
                <div className="profile-hero-background-layer" style={createProfileHeroBackgroundStyle(editDraft.backgroundImageUrl)} />
                <div className="profile-hero-backdrop" aria-hidden="true" />
                {!editDraft.backgroundImageUrl ? <span className="profile-hero-default-background-label">{profileSummary.handle}</span> : null}

                {isProfileCardEditing ? (
                  <div className="profile-hero-image-overlay is-cover">
                    <label className="profile-hero-overlay-button profile-hero-card-upload-action">
                      <input
                        type="file"
                        accept={PROFILE_IMAGE_ACCEPT}
                        onChange={handleProfileBackgroundImageSelect}
                        disabled={isProfileBackgroundUploading}
                      />
                      <EditIcon />
                    </label>
                    {editDraft.backgroundImageUrl ? (
                      <button
                        type="button"
                        className="profile-hero-overlay-button is-delete"
                        aria-label="배경 삭제"
                        title="배경 삭제"
                        onClick={() =>
                          updateDraft((draft) => ({
                            ...draft,
                            backgroundImageUrl: '',
                          }))
                        }
                      >
                        <TrashIcon />
                      </button>
                    ) : null}
                  </div>
                ) : null}
              </div>

              <div className="profile-hero-body profile-hero-body-edit">
                <div className="profile-hero-avatar-shell profile-hero-avatar-shell-edit">
                  <div className="profile-editor-avatar-column profile-editor-avatar-column-in-card">
                    <div className={`profile-hero-avatar-frame ${isProfileCardEditing ? 'is-editing' : ''}`.trim()}>
                      {renderProfileAvatar(editDraft.profileImageUrl, profileSummary.handle, 'profile-editor-avatar')}

                      {isProfileCardEditing ? (
                        <div className="profile-hero-image-overlay is-avatar">
                          <label className="profile-hero-overlay-button profile-hero-card-upload-action">
                            <input type="file" accept={PROFILE_IMAGE_ACCEPT} onChange={handleProfileImageSelect} disabled={isProfileImageUploading} />
                            <EditIcon />
                          </label>
                          {editDraft.profileImageUrl ? (
                            <button
                              type="button"
                              className="profile-hero-overlay-button is-delete"
                              aria-label="프로필 사진 삭제"
                              title="프로필 사진 삭제"
                              onClick={() =>
                                updateDraft((draft) => ({
                                  ...draft,
                                  profileImageUrl: '',
                                }))
                              }
                            >
                              <TrashIcon />
                            </button>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>

                <div className={`profile-hero-copy-next profile-hero-copy-next-edit ${editDraft.bio.trim() === '' ? 'is-bio-empty' : ''}`.trim()}>
                  <div className="profile-hero-title-row">
                    <h1 className="page-title profile-page-title">{profileSummary.handle}</h1>
                    {editingHeroLinks.length > 0 ? (
                      <div className="profile-hero-link-row profile-hero-link-row-inline">
                        <div className="profile-hero-inline-link-list" aria-label="외부 링크">
                          {createHeroLinkNodes(editingHeroLinks)}
                        </div>
                      </div>
                    ) : null}
                  </div>
                  {editDraft.bio.trim() !== '' ? <p className="profile-hero-bio is-edit-preview">{editDraft.bio}</p> : null}
                </div>
              </div>
              </section>
            </div>

            <div className="profile-editor-block is-bio">
              <div className="profile-editor-block-heading">
                <p className="field-label">소개</p>
                <div className="profile-editor-heading-actions">
                  {isBioEditing ? (
                    <>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-save-button"
                        aria-label="소개 편집 완료"
                        title="완료"
                        disabled={savingSection === 'bio'}
                        onClick={() => void completeBioEditing()}
                      >
                        <CheckIcon />
                      </button>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-cancel-button"
                        aria-label="소개 편집 취소"
                        title="취소"
                        disabled={savingSection === 'bio'}
                        onClick={cancelBioEditing}
                      >
                        <CloseIcon />
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn text profile-editor-icon-button"
                      aria-label="소개 수정"
                      title="수정"
                      disabled={isProfileEditorBusy}
                      onClick={startBioEditing}
                    >
                      <EditIcon />
                    </button>
                  )}
                </div>
              </div>

              <div className="profile-editor-link-table" role="table" aria-label="프로필 소개">
                <div className="profile-editor-link-row profile-editor-bio-row is-single-column" role="row">
                  {isBioEditing ? (
                    <input
                      type="text"
                      className="text-field profile-editor-link-input profile-editor-bio-input"
                      value={editDraft.bio}
                      maxLength={80}
                      onChange={(event) =>
                        updateDraft((draft) => ({
                          ...draft,
                          bio: event.target.value,
                        }))
                      }
                      placeholder="소개"
                      aria-label="소개"
                    />
                  ) : (
                    <div className="profile-editor-config-display profile-editor-bio-display-row">
                      {editDraft.bio.trim() === '' ? '' : editDraft.bio}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="profile-editor-block is-links">
              <div className="profile-editor-block-heading">
                <p className="field-label">링크</p>
                <div className="profile-editor-heading-actions">
                  {isLinkBatchEditing ? (
                    <>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-save-button"
                        aria-label="링크 편집 완료"
                        title="완료"
                        disabled={savingSection === 'links'}
                        onClick={() => void completeAllLinksEditing()}
                      >
                        <CheckIcon />
                      </button>
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button profile-editor-cancel-button"
                        aria-label="링크 편집 취소"
                        title="취소"
                        disabled={savingSection === 'links'}
                        onClick={cancelAllLinksEditing}
                      >
                        <CloseIcon />
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn text profile-editor-icon-button"
                      aria-label="링크 수정"
                      title="수정"
                      disabled={isProfileEditorBusy}
                      onClick={startAllLinksEditing}
                    >
                      <EditIcon />
                    </button>
                  )}
                </div>
              </div>

              <div className="profile-editor-link-table" role="table" aria-label="프로필 링크">
                <div className="profile-editor-link-head" role="row">
                  <span role="columnheader">키 (Label)</span>
                  <span role="columnheader">URL</span>
                  <span role="columnheader" className="profile-editor-link-head-action">
                    <button
                      type="button"
                      className="btn text profile-editor-add-link-button"
                      disabled={savingSection === 'links' || !isLinkBatchEditing || editDraft.links.length >= 10}
                      onClick={addLinkDraft}
                      aria-label="링크 추가"
                      title="링크 추가"
                    >
                      <PlusIcon />
                    </button>
                  </span>
                </div>

                {editDraft.links.length === 0 ? <div className="profile-editor-config-empty">등록된 링크 없음</div> : null}

                {editDraft.links.map((link, index) => (
                  <div key={`profile-link-${index}`} className="profile-editor-link-row" role="row">
                    {isLinkBatchEditing ? (
                      <>
                        <input
                          className="text-field profile-editor-link-input"
                          value={link.type}
                          onChange={(event) => updateLinkDraft(index, 'type', event.target.value)}
                          placeholder="Blog"
                          aria-label="링크 라벨"
                        />
                        <input
                          className="text-field profile-editor-link-input"
                          value={link.value}
                          onChange={(event) => updateLinkDraft(index, 'value', event.target.value)}
                          placeholder="https://"
                          aria-label="링크 URL"
                        />
                      </>
                    ) : (
                      <>
                        <div className="profile-editor-config-display">{link.type.trim() === '' ? '-' : link.type}</div>
                        <div className="profile-editor-config-display profile-editor-config-display-multiline">{link.value.trim() === '' ? '-' : link.value}</div>
                      </>
                    )}

                    <div className="profile-editor-link-actions">
                      {isLinkBatchEditing ? (
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-delete-button"
                          aria-label="링크 삭제"
                          title="삭제"
                          disabled={savingSection === 'links'}
                          onClick={() => removeLinkDraft(index)}
                        >
                          <TrashIcon />
                        </button>
                      ) : (
                        <span className="profile-editor-link-action-placeholder" aria-hidden="true" />
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="profile-editor-settings">
              <div className="profile-editor-setting-block">
                <div className="profile-editor-setting-heading">
                  <p className="field-label">기본 DBMS</p>
                  <div className="profile-editor-heading-actions">
                    {editingSettingKey === 'defaultDbms' ? (
                      <>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-save-button"
                          aria-label="기본 DBMS 저장"
                          title="완료"
                          disabled={savingSection === 'defaultDbms'}
                          onClick={() => void completeSettingEditing('defaultDbms')}
                        >
                          <CheckIcon />
                        </button>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-cancel-button"
                          aria-label="기본 DBMS 편집 취소"
                          title="취소"
                          disabled={savingSection === 'defaultDbms'}
                          onClick={() => cancelSettingEditing('defaultDbms')}
                        >
                          <CloseIcon />
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button"
                        aria-label="기본 DBMS 수정"
                        title="수정"
                        disabled={isProfileEditorBusy}
                        onClick={() => startSettingEditing('defaultDbms')}
                      >
                        <EditIcon />
                      </button>
                    )}
                  </div>
                </div>
                <div
                  className={`profile-editor-radio-row ${editingSettingKey === 'defaultDbms' ? 'is-editing' : 'is-locked'}`.trim()}
                  role="radiogroup"
                  aria-label="기본 DBMS 선택"
                >
                  {dbmsOptions.map((option) => (
                    <label key={option.value} className={`profile-editor-radio-label ${editDraft.defaultDbms === option.value ? 'is-selected' : ''}`.trim()}>
                      <input
                        type="radio"
                        name="profile-default-dbms"
                        disabled={editingSettingKey !== 'defaultDbms'}
                        checked={editDraft.defaultDbms === option.value}
                        onChange={() =>
                          updateDraft((draft) => ({
                            ...draft,
                            defaultDbms: option.value,
                          }))
                        }
                      />
                      <span>{option.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="profile-editor-setting-block">
                <div className="profile-editor-setting-heading">
                  <p className="field-label">작성한 SQL 공개 여부</p>
                  <div className="profile-editor-heading-actions">
                    {editingSettingKey === 'sqlPublic' ? (
                      <>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-save-button"
                          aria-label="작성한 SQL 공개 여부 저장"
                          title="완료"
                          disabled={savingSection === 'sqlPublic'}
                          onClick={() => void completeSettingEditing('sqlPublic')}
                        >
                          <CheckIcon />
                        </button>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-cancel-button"
                          aria-label="작성한 SQL 공개 여부 편집 취소"
                          title="취소"
                          disabled={savingSection === 'sqlPublic'}
                          onClick={() => cancelSettingEditing('sqlPublic')}
                        >
                          <CloseIcon />
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button"
                        aria-label="작성한 SQL 공개 여부 수정"
                        title="수정"
                        disabled={isProfileEditorBusy}
                        onClick={() => startSettingEditing('sqlPublic')}
                      >
                        <EditIcon />
                      </button>
                    )}
                  </div>
                </div>
                <div
                  className={`profile-editor-radio-row ${editingSettingKey === 'sqlPublic' ? 'is-editing' : 'is-locked'}`.trim()}
                  role="radiogroup"
                  aria-label="작성한 SQL 공개 여부"
                >
                  <label className={`profile-editor-radio-label ${editDraft.sqlPublic ? 'is-selected' : ''}`.trim()}>
                    <input
                      type="radio"
                      name="profile-sql-public"
                      disabled={editingSettingKey !== 'sqlPublic'}
                      checked={editDraft.sqlPublic}
                      onChange={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          sqlPublic: true,
                        }))
                      }
                    />
                    <span>공개</span>
                  </label>
                  <label className={`profile-editor-radio-label ${!editDraft.sqlPublic ? 'is-selected' : ''}`.trim()}>
                    <input
                      type="radio"
                      name="profile-sql-public"
                      disabled={editingSettingKey !== 'sqlPublic'}
                      checked={!editDraft.sqlPublic}
                      onChange={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          sqlPublic: false,
                        }))
                      }
                    />
                    <span>비공개</span>
                  </label>
                </div>
              </div>

              <div className="profile-editor-setting-block">
                <div className="profile-editor-setting-heading">
                  <p className="field-label">커뮤니티 활동 공개 여부</p>
                  <div className="profile-editor-heading-actions">
                    {editingSettingKey === 'communityActivityPublic' ? (
                      <>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-save-button"
                          aria-label="커뮤니티 활동 공개 여부 저장"
                          title="완료"
                          disabled={savingSection === 'communityActivityPublic'}
                          onClick={() => void completeSettingEditing('communityActivityPublic')}
                        >
                          <CheckIcon />
                        </button>
                        <button
                          type="button"
                          className="btn text profile-editor-icon-button profile-editor-cancel-button"
                          aria-label="커뮤니티 활동 공개 여부 편집 취소"
                          title="취소"
                          disabled={savingSection === 'communityActivityPublic'}
                          onClick={() => cancelSettingEditing('communityActivityPublic')}
                        >
                          <CloseIcon />
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="btn text profile-editor-icon-button"
                        aria-label="커뮤니티 활동 공개 여부 수정"
                        title="수정"
                        disabled={isProfileEditorBusy}
                        onClick={() => startSettingEditing('communityActivityPublic')}
                      >
                        <EditIcon />
                      </button>
                    )}
                  </div>
                </div>
                <div
                  className={`profile-editor-radio-row ${editingSettingKey === 'communityActivityPublic' ? 'is-editing' : 'is-locked'}`.trim()}
                  role="radiogroup"
                  aria-label="커뮤니티 활동 공개 여부"
                >
                  <label className={`profile-editor-radio-label ${editDraft.communityActivityPublic ? 'is-selected' : ''}`.trim()}>
                    <input
                      type="radio"
                      name="profile-community-public"
                      disabled={editingSettingKey !== 'communityActivityPublic'}
                      checked={editDraft.communityActivityPublic}
                      onChange={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          communityActivityPublic: true,
                        }))
                      }
                    />
                    <span>공개</span>
                  </label>
                  <label className={`profile-editor-radio-label ${!editDraft.communityActivityPublic ? 'is-selected' : ''}`.trim()}>
                    <input
                      type="radio"
                      name="profile-community-public"
                      disabled={editingSettingKey !== 'communityActivityPublic'}
                      checked={!editDraft.communityActivityPublic}
                      onChange={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          communityActivityPublic: false,
                        }))
                      }
                    />
                    <span>비공개</span>
                  </label>
                </div>
              </div>
            </div>
          </section>
        ) : isAlarmListOpen ? (
          renderAlarmSection()
        ) : (
          <section className={`panel-card profile-main-shell ${isLoading ? 'is-loading' : ''}`.trim()}>
            <section className={`panel-card profile-hero-panel ${isLoading ? 'is-loading' : ''}`.trim()}>
              <div className={`profile-hero-cover ${profileSummary.backgroundImageUrl ? '' : 'is-default-background'}`.trim()}>
                <div className="profile-hero-background-layer" style={createProfileHeroBackgroundStyle(profileSummary.backgroundImageUrl)} />
                <div className="profile-hero-backdrop" aria-hidden="true" />
                {!profileSummary.backgroundImageUrl ? <span className="profile-hero-default-background-label">{profileSummary.handle}</span> : null}
              </div>

              <div className="profile-hero-body">
                <div className="profile-hero-avatar-shell">
                  {renderProfileAvatar(profileSummary.profileImageUrl, profileSummary.handle)}
                </div>

                <div className={`profile-hero-copy-next ${profileSummary.bio.trim() === '' ? 'is-bio-empty' : ''}`.trim()}>
                  <div className="profile-hero-title-row">
                    <h1 className="page-title profile-page-title">{profileSummary.handle}</h1>
                    {heroLinks.length > 0 ? (
                      <div className="profile-hero-link-row profile-hero-link-row-inline">
                        <div className="profile-hero-inline-link-list" aria-label="외부 링크">
                          {createHeroLinkNodes(heroLinks)}
                        </div>
                      </div>
                    ) : null}
                  </div>

                  {profileSummary.bio.trim() !== '' ? <p className="profile-hero-bio">{profileSummary.bio}</p> : null}
                </div>
              </div>
            </section>

            <div className="profile-main-content profile-profile-flow">
              {renderSummarySection()}
              {renderSolveSection()}
              {renderCommunitySection()}
            </div>

            {isLoading ? <div className="submit-history-loading-overlay" aria-hidden="true" /> : null}
          </section>
        )}
      </div>

      {imageCropState ? (
        <ImageCropModal
          key={`${imageCropState.target}:${imageCropState.sourceUrl}:${imageCropState.target === 'background' ? profileBackgroundCropAspect : 1}`}
          ariaLabel={imageCropState.target === 'background' ? '프로필 배경 자르기' : '프로필 사진 자르기'}
          imageSrc={imageCropState.sourceUrl}
          aspect={imageCropState.target === 'background' ? profileBackgroundCropAspect : 1}
          cropShape={imageCropState.target === 'background' ? 'rect' : 'round'}
          objectFit={imageCropState.target === 'background' ? 'horizontal-cover' : 'contain'}
          minZoom={1}
          initialZoom={1}
          maxZoom={4}
          isApplying={isImageCropApplying}
          onCancel={closeImageCropModal}
          onApply={applyImageCrop}
        />
      ) : null}
    </>
  );
}
