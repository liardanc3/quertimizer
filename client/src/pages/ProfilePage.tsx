import { useEffect, useState } from 'react';
import { PROFILE_ACTIVITY_PATH, getProfilePath, navigate } from '../lib/navigation';
import { getMockProfileByHandle, mockCurrentHandle, mockProfiles } from '../mocks/profile';
import type {
  DbmsType,
  Profile,
  ProfileLinks,
  ProfileSettings,
  ProfileSolvedRecord,
  SqlEditorPreset,
  SqlVisibility,
} from '../types/domain';

interface ProfilePageProps {
  handle?: string;
}

interface ProfileEditDraft {
  avatarUrl: string;
  bio: string;
  links: ProfileLinks;
}

interface PasswordDraft {
  currentPassword: string;
  nextPassword: string;
  confirmPassword: string;
}

interface FeedbackState {
  tone: 'success' | 'error';
  message: string;
}

type SortKey = 'problemNumber' | 'executionTimeMs' | 'solvedAt';
type SortDirection = 'asc' | 'desc';
type ProfileLinkKind = keyof ProfileLinks;

const numberFormatter = new Intl.NumberFormat('ko-KR');
const dateFormatter = new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' });

const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];

const editorPresetOptions: Array<{ value: SqlEditorPreset; label: string; description: string }> = [
  { value: 'focused', label: '집중형', description: '에디터 중심으로 빠르게 풀이' },
  { value: 'balanced', label: '균형형', description: '에디터와 결과 확인 균형' },
  { value: 'analysis', label: '분석형', description: '실행계획과 튜닝 점검에 최적화' },
];

const visibilityOptions: Array<{ value: SqlVisibility; label: string; description: string }> = [
  { value: 'public', label: '공개', description: '누구나 SQL 풀이를 볼 수 있습니다.' },
  { value: 'followers', label: '팔로우만', description: '승인된 사람에게만 풀이를 공개합니다.' },
  { value: 'private', label: '비공개', description: '본인만 SQL 기록을 확인합니다.' },
];

const linkMeta: Array<{ kind: ProfileLinkKind; label: string; placeholder: string }> = [
  { kind: 'blog', label: 'BLOG', placeholder: 'blog.example.dev' },
  { kind: 'github', label: 'GITHUB', placeholder: 'github.com/handle' },
  { kind: 'email', label: 'EMAIL', placeholder: 'hello@example.com' },
];

function createEditDraft(profile: Profile): ProfileEditDraft {
  return {
    avatarUrl: profile.avatarUrl ?? '',
    bio: profile.bio,
    links: {
      blog: profile.links.blog ?? '',
      github: profile.links.github ?? '',
      email: profile.links.email ?? '',
    },
  };
}

function createPasswordDraft(): PasswordDraft {
  return {
    currentPassword: '',
    nextPassword: '',
    confirmPassword: '',
  };
}

function normalizeLinkValue(value?: string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function normalizeExternalHref(kind: ProfileLinkKind, value: string) {
  if (kind === 'email') {
    return value.startsWith('mailto:') ? value : `mailto:${value}`;
  }

  if (value.startsWith('http://') || value.startsWith('https://')) {
    return value;
  }

  return `https://${value}`;
}

function getDisplayLinkValue(kind: ProfileLinkKind, value: string) {
  if (kind === 'email') {
    return value.replace(/^mailto:/, '');
  }

  return value.replace(/^https?:\/\//, '');
}

function getInitials(profile: Pick<Profile, 'handle' | 'name'>) {
  const source = profile.handle || profile.name || 'SQ';
  return source
    .replace(/[^a-zA-Z0-9가-힣]/g, '')
    .slice(0, 2)
    .toUpperCase();
}

function getAverageExecutionTime(records: ProfileSolvedRecord[]) {
  if (records.length === 0) {
    return 0;
  }

  return records.reduce((sum, record) => sum + record.executionTimeMs, 0) / records.length;
}

function getLatestSolvedAt(records: ProfileSolvedRecord[]) {
  if (records.length === 0) {
    return undefined;
  }

  return records.reduce((latest, record) => {
    if (!latest) {
      return record.solvedAt;
    }

    return new Date(record.solvedAt).getTime() > new Date(latest).getTime() ? record.solvedAt : latest;
  }, records[0]?.solvedAt);
}

function matchesRecordSearch(record: ProfileSolvedRecord, query: string) {
  if (!query) {
    return true;
  }

  const normalizedQuery = query.trim().toLowerCase();
  return (
    String(record.problemNumber).includes(normalizedQuery) ||
    record.problemTitle.toLowerCase().includes(normalizedQuery) ||
    dateFormatter.format(new Date(record.solvedAt)).toLowerCase().includes(normalizedQuery)
  );
}

function sortRecords(records: ProfileSolvedRecord[], sortKey: SortKey, sortDirection: SortDirection) {
  return [...records].sort((left, right) => {
    let compareValue = 0;

    if (sortKey === 'problemNumber') {
      compareValue = left.problemNumber - right.problemNumber;
    }

    if (sortKey === 'executionTimeMs') {
      compareValue = left.executionTimeMs - right.executionTimeMs;
    }

    if (sortKey === 'solvedAt') {
      compareValue = new Date(left.solvedAt).getTime() - new Date(right.solvedAt).getTime();
    }

    if (compareValue === 0) {
      compareValue = left.problemNumber - right.problemNumber;
    }

    return sortDirection === 'asc' ? compareValue : -compareValue;
  });
}

function SortButton({
  active,
  direction,
  label,
  onClick,
}: {
  active: boolean;
  direction: SortDirection;
  label: string;
  onClick: () => void;
}) {
  return (
    <button type="button" className={`profile-sort-button ${active ? 'is-active' : ''}`} onClick={onClick}>
      <span>{label}</span>
      <span className="profile-sort-icon" aria-hidden="true">
        {active ? (direction === 'asc' ? '↑' : '↓') : '↕'}
      </span>
    </button>
  );
}

function ProfileAvatar({
  profile,
  src,
  className,
}: {
  profile: Pick<Profile, 'handle' | 'name'>;
  src?: string;
  className: string;
}) {
  const [hasImageError, setHasImageError] = useState(false);
  const normalizedSrc = src?.trim();

  useEffect(() => {
    setHasImageError(false);
  }, [normalizedSrc]);

  if (!normalizedSrc || hasImageError) {
    return (
      <div className={`${className} is-fallback`} aria-hidden="true">
        {getInitials(profile)}
      </div>
    );
  }

  return (
    <img
      className={className}
      src={normalizedSrc}
      alt={`${profile.handle} 프로필`}
      onError={() => setHasImageError(true)}
    />
  );
}

function NotFoundState() {
  return (
    <div className="page-stack">
      <section className="panel-card">
        <p className="panel-meta">프로필</p>
        <h1 className="page-title">프로필을 찾을 수 없습니다.</h1>
        <p className="muted-text">등록된 핸들을 다시 확인하거나 아래 공개 프로필로 이동해 주세요.</p>
      </section>

      <section className="panel-card compact">
        <div className="profile-discovery-list">
          {mockProfiles.map((profile) => (
            <button
              key={profile.handle}
              type="button"
              className="mini-toggle profile-discovery-button"
              onClick={() => navigate(getProfilePath(profile.handle))}
            >
              @{profile.handle}
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

export default function ProfilePage({ handle }: ProfilePageProps) {
  const sourceProfile = getMockProfileByHandle(handle);
  const [profile, setProfile] = useState<Profile | null>(sourceProfile ?? null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [editDraft, setEditDraft] = useState<ProfileEditDraft | null>(sourceProfile ? createEditDraft(sourceProfile) : null);
  const [settingsDraft, setSettingsDraft] = useState<ProfileSettings | null>(sourceProfile?.settings ?? null);
  const [passwordDraft, setPasswordDraft] = useState(createPasswordDraft);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('solvedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  if (!sourceProfile || !profile || !editDraft || !settingsDraft) {
    return <NotFoundState />;
  }

  const isOwnProfile = profile.handle === mockCurrentHandle;
  const visibleLinks = linkMeta
    .map(({ kind, label }) => {
      const rawValue = profile.links[kind];

      if (!rawValue) {
        return null;
      }

      return {
        kind,
        label,
        href: normalizeExternalHref(kind, rawValue),
        displayValue: getDisplayLinkValue(kind, rawValue),
      };
    })
    .filter((link): link is NonNullable<typeof link> => Boolean(link));

  const solvedNumbers = [...profile.solvedProblems].sort((left, right) => left.problemNumber - right.problemNumber);
  const filteredRecords = profile.solvedProblems.filter((record) => matchesRecordSearch(record, searchQuery));
  const sortedRecords = sortRecords(filteredRecords, sortKey, sortDirection);
  const averageExecutionTime = getAverageExecutionTime(profile.solvedProblems);
  const latestSolvedAt = getLatestSolvedAt(profile.solvedProblems);

  function toggleSort(nextKey: SortKey) {
    if (sortKey === nextKey) {
      setSortDirection((direction) => (direction === 'asc' ? 'desc' : 'asc'));
      return;
    }

    setSortKey(nextKey);
    setSortDirection(nextKey === 'problemNumber' ? 'asc' : 'desc');
  }

  function openEditPanel() {
    if (!profile) {
      return;
    }

    setEditDraft(createEditDraft(profile));
    setFeedback(null);
    setIsSettingsOpen(false);
    setIsEditOpen((open) => !open);
  }

  function openSettingsPanel() {
    if (!profile) {
      return;
    }

    setSettingsDraft(profile.settings);
    setPasswordDraft(createPasswordDraft());
    setFeedback(null);
    setIsEditOpen(false);
    setIsSettingsOpen((open) => !open);
  }

  function saveProfileChanges() {
    if (!profile || !editDraft) {
      return;
    }

    const nextProfile: Profile = {
      ...profile,
      avatarUrl: normalizeLinkValue(editDraft.avatarUrl),
      bio: editDraft.bio.trim() || '소개글이 아직 등록되지 않았습니다.',
      links: {
        blog: normalizeLinkValue(editDraft.links.blog),
        github: normalizeLinkValue(editDraft.links.github),
        email: normalizeLinkValue(editDraft.links.email),
      },
    };

    setProfile(nextProfile);
    setEditDraft(createEditDraft(nextProfile));
    setIsEditOpen(false);
    setFeedback({ tone: 'success', message: '프로필 정보가 저장되었습니다. (mock)' });
  }

  function saveSettingsChanges() {
    if (!profile || !settingsDraft) {
      return;
    }

    const passwordRequested =
      passwordDraft.currentPassword || passwordDraft.nextPassword || passwordDraft.confirmPassword;

    if (passwordRequested) {
      if (!passwordDraft.currentPassword || !passwordDraft.nextPassword || !passwordDraft.confirmPassword) {
        setFeedback({ tone: 'error', message: '비밀번호 변경 항목을 모두 입력해 주세요.' });
        return;
      }

      if (passwordDraft.nextPassword.length < 8) {
        setFeedback({ tone: 'error', message: '새 비밀번호는 8자 이상으로 입력해 주세요.' });
        return;
      }

      if (passwordDraft.nextPassword !== passwordDraft.confirmPassword) {
        setFeedback({ tone: 'error', message: '새 비밀번호와 확인 값이 일치하지 않습니다.' });
        return;
      }
    }

    const nextProfile: Profile = {
      ...profile,
      settings: settingsDraft,
    };

    setProfile(nextProfile);
    setSettingsDraft(nextProfile.settings);
    setPasswordDraft(createPasswordDraft());
    setIsSettingsOpen(false);
    setFeedback({
      tone: 'success',
      message: passwordRequested ? '개인 설정과 비밀번호 변경이 저장되었습니다. (mock)' : '개인 설정이 저장되었습니다. (mock)',
    });
  }

  return (
    <div className="page-stack">
      <section className="panel-card profile-hero-card">
        <div className="profile-hero-layout">
          <ProfileAvatar profile={profile} src={profile.avatarUrl} className="profile-page-avatar profile-page-avatar-lg" />

          <div className="profile-hero-copy">
            <div className="profile-hero-meta-row">
              <p className="panel-meta">프로필</p>
              <span className="subtle-chip">{isOwnProfile ? '내 프로필' : '공개 프로필'}</span>
            </div>

            <h1 className="page-title">@{profile.handle}</h1>
            <p className="profile-name-line">
              {profile.name} · {profile.tier}
            </p>
            <p className="profile-bio-text">{profile.bio}</p>

            {visibleLinks.length > 0 ? (
              <div className="profile-link-list">
                {visibleLinks.map((link) => (
                  <a
                    key={link.kind}
                    className="profile-link-chip"
                    href={link.href}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <span className="profile-link-label">{link.label}</span>
                    <span className="profile-link-value">{link.displayValue}</span>
                  </a>
                ))}
              </div>
            ) : null}
          </div>

          {isOwnProfile ? (
            <div className="profile-hero-actions">
              <button type="button" className="btn ghost" onClick={() => navigate(PROFILE_ACTIVITY_PATH)}>
                내 활동
              </button>
              <button type="button" className="btn secondary" onClick={openEditPanel}>
                {isEditOpen ? '편집 닫기' : '프로필 편집'}
              </button>
              <button type="button" className="btn ghost" onClick={openSettingsPanel}>
                {isSettingsOpen ? '설정 닫기' : '설정'}
              </button>
            </div>
          ) : null}
        </div>
      </section>

      {feedback ? (
        <section className={`panel-card compact profile-feedback-card is-${feedback.tone}`}>
          <p className="profile-feedback-text">{feedback.message}</p>
        </section>
      ) : null}

      <section className="panel-card compact">
        <div className="profile-summary-grid">
          <article className="profile-summary-card">
            <p className="stat-label">해결한 문제 수</p>
            <p className="profile-summary-value">{numberFormatter.format(profile.solvedCount)}</p>
          </article>
          <article className="profile-summary-card">
            <p className="stat-label">평균 실행 시간</p>
            <p className="profile-summary-value">{averageExecutionTime.toFixed(1)} ms</p>
          </article>
          <article className="profile-summary-card">
            <p className="stat-label">최근 해결 날짜</p>
            <p className="profile-summary-value">{latestSolvedAt ? dateFormatter.format(new Date(latestSolvedAt)) : '-'}</p>
          </article>
        </div>
      </section>

      {isOwnProfile && isEditOpen ? (
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">프로필 편집</p>
              <h2 className="panel-title">프로필 편집</h2>
            </div>
            <span className="subtle-chip">사진 · 소개글 · 외부 링크</span>
          </div>

          <div className="profile-editor-layout">
            <div className="profile-editor-preview">
              <ProfileAvatar profile={profile} src={editDraft.avatarUrl} className="profile-page-avatar profile-page-avatar-md" />
              <p className="hint-text">이미지 URL을 비워두면 기본 아바타가 표시됩니다.</p>
            </div>

            <div className="profile-editor-fields">
              <label className="field-stack">
                <span className="field-label">프로필 사진 URL</span>
                <input
                  className="text-field"
                  value={editDraft.avatarUrl}
                  onChange={(event) =>
                    setEditDraft((draft) =>
                      draft
                        ? {
                            ...draft,
                            avatarUrl: event.target.value,
                          }
                        : draft
                    )
                  }
                  placeholder="/favicon.svg 또는 https://..."
                />
              </label>

              <label className="field-stack">
                <span className="field-label">소개글</span>
                <textarea
                  className="text-field profile-textarea"
                  value={editDraft.bio}
                  onChange={(event) =>
                    setEditDraft((draft) =>
                      draft
                        ? {
                            ...draft,
                            bio: event.target.value,
                          }
                        : draft
                    )
                  }
                  placeholder="사용하는 DBMS, 관심 있는 SQL 주제, 학습 스타일을 소개해 보세요."
                />
              </label>

              <div className="profile-link-editor-grid">
                {linkMeta.map((link) => (
                  <label key={link.kind} className="field-stack">
                    <span className="field-label">{link.label}</span>
                    <input
                      className="text-field"
                      value={editDraft.links[link.kind] ?? ''}
                      onChange={(event) =>
                        setEditDraft((draft) =>
                          draft
                            ? {
                                ...draft,
                                links: {
                                  ...draft.links,
                                  [link.kind]: event.target.value,
                                },
                              }
                            : draft
                        )
                      }
                      placeholder={link.placeholder}
                    />
                  </label>
                ))}
              </div>

              <div className="auth-actions">
                <button type="button" className="btn primary" onClick={saveProfileChanges}>
                  저장
                </button>
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => {
                    setEditDraft(createEditDraft(profile));
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

      {isOwnProfile && isSettingsOpen ? (
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">설정</p>
              <h2 className="panel-title">개인 설정</h2>
            </div>
            <span className="subtle-chip">RDBMS · 에디터 · 공개 범위 · 비밀번호</span>
          </div>

          <div className="profile-settings-stack">
            <div className="profile-settings-group">
              <div>
                <p className="field-label">기본 RDBMS</p>
                <p className="hint-text">문제 풀이 화면에 처음 선택될 DBMS입니다.</p>
              </div>
              <div className="segmented profile-settings-segmented">
                {dbmsOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`segmented-btn ${settingsDraft.defaultDbms === option.value ? 'is-selected' : ''}`}
                    onClick={() =>
                      setSettingsDraft((draft) =>
                        draft
                          ? {
                              ...draft,
                              defaultDbms: option.value,
                            }
                          : draft
                      )
                    }
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-settings-group">
              <div>
                <p className="field-label">SQL 에디터 기본값</p>
                <p className="hint-text">원하는 풀이 스타일에 맞춰 기본 에디터 프리셋을 선택합니다.</p>
              </div>
              <div className="profile-option-grid">
                {editorPresetOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`profile-option-card ${settingsDraft.sqlEditorPreset === option.value ? 'is-selected' : ''}`}
                    onClick={() =>
                      setSettingsDraft((draft) =>
                        draft
                          ? {
                              ...draft,
                              sqlEditorPreset: option.value,
                            }
                          : draft
                      )
                    }
                  >
                    <strong>{option.label}</strong>
                    <span>{option.description}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-settings-group">
              <div>
                <p className="field-label">SQL 공개 설정</p>
                <p className="hint-text">제출한 SQL 풀이와 기록의 기본 공개 범위를 지정합니다.</p>
              </div>
              <div className="profile-option-grid">
                {visibilityOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`profile-option-card ${settingsDraft.sqlVisibility === option.value ? 'is-selected' : ''}`}
                    onClick={() =>
                      setSettingsDraft((draft) =>
                        draft
                          ? {
                              ...draft,
                              sqlVisibility: option.value,
                            }
                          : draft
                      )
                    }
                  >
                    <strong>{option.label}</strong>
                    <span>{option.description}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-settings-group">
              <div>
                <p className="field-label">비밀번호 변경</p>
                <p className="hint-text">변경이 필요할 때만 입력하면 됩니다.</p>
              </div>
              <div className="profile-password-grid">
                <label className="field-stack">
                  <span className="field-label">현재 비밀번호</span>
                  <input
                    type="password"
                    className="text-field"
                    value={passwordDraft.currentPassword}
                    onChange={(event) =>
                      setPasswordDraft((draft) => ({
                        ...draft,
                        currentPassword: event.target.value,
                      }))
                    }
                    placeholder="현재 비밀번호"
                  />
                </label>
                <label className="field-stack">
                  <span className="field-label">새 비밀번호</span>
                  <input
                    type="password"
                    className="text-field"
                    value={passwordDraft.nextPassword}
                    onChange={(event) =>
                      setPasswordDraft((draft) => ({
                        ...draft,
                        nextPassword: event.target.value,
                      }))
                    }
                    placeholder="8자 이상"
                  />
                </label>
                <label className="field-stack">
                  <span className="field-label">새 비밀번호 확인</span>
                  <input
                    type="password"
                    className="text-field"
                    value={passwordDraft.confirmPassword}
                    onChange={(event) =>
                      setPasswordDraft((draft) => ({
                        ...draft,
                        confirmPassword: event.target.value,
                      }))
                    }
                    placeholder="새 비밀번호를 다시 입력"
                  />
                </label>
              </div>
            </div>

            <div className="auth-actions">
              <button type="button" className="btn primary" onClick={saveSettingsChanges}>
                설정 저장
              </button>
              <button
                type="button"
                className="btn ghost"
                onClick={() => {
                  setSettingsDraft(profile.settings);
                  setPasswordDraft(createPasswordDraft());
                  setIsSettingsOpen(false);
                }}
              >
                취소
              </button>
            </div>
          </div>
        </section>
      ) : null}

      <section className="panel-card">
        <div className="panel-heading-row responsive">
          <div>
            <p className="panel-meta">해결한 문제</p>
            <h2 className="panel-title">해결한 문제 번호</h2>
          </div>
          <span className="subtle-chip">문제 {profile.solvedCount}개</span>
        </div>

        <div className="profile-problem-chip-list">
          {solvedNumbers.map((record) => (
            <button
              key={record.id}
              type="button"
              className="mini-toggle profile-problem-chip"
              onClick={() => navigate(`/problems/${record.problemId}`)}
            >
              #{record.problemNumber}
            </button>
          ))}
        </div>
      </section>

      <section className="panel-card">
        <div className="panel-heading-row responsive profile-records-toolbar">
          <div>
            <p className="panel-meta">해결 기록</p>
            <h2 className="panel-title">해결 기록</h2>
          </div>

          <label className="profile-record-search">
            <span className="profile-record-search-label">검색</span>
            <input
              type="search"
              className="text-field"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="문제 번호, 제목, 해결 날짜 검색"
              aria-label="해결 기록 검색"
            />
          </label>
        </div>

        {sortedRecords.length > 0 ? (
          <div className="profile-records-scroll">
            <table className="profile-records-table">
              <thead>
                <tr>
                  <th>
                    <SortButton
                      active={sortKey === 'problemNumber'}
                      direction={sortDirection}
                      label="문제 번호"
                      onClick={() => toggleSort('problemNumber')}
                    />
                  </th>
                  <th>
                    <SortButton
                      active={sortKey === 'executionTimeMs'}
                      direction={sortDirection}
                      label="실행 시간"
                      onClick={() => toggleSort('executionTimeMs')}
                    />
                  </th>
                  <th>
                    <SortButton
                      active={sortKey === 'solvedAt'}
                      direction={sortDirection}
                      label="해결 날짜"
                      onClick={() => toggleSort('solvedAt')}
                    />
                  </th>
                </tr>
              </thead>
              <tbody>
                {sortedRecords.map((record) => (
                  <tr key={record.id}>
                    <td>
                      <button
                        type="button"
                        className="btn text inline profile-record-link"
                        onClick={() => navigate(`/problems/${record.problemId}`)}
                      >
                        <span>#{record.problemNumber}</span>
                        <span className="profile-record-title">{record.problemTitle}</span>
                      </button>
                    </td>
                    <td>{record.executionTimeMs.toFixed(1)} ms</td>
                    <td>{dateFormatter.format(new Date(record.solvedAt))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="profile-empty-state">검색 조건에 맞는 해결 기록이 없습니다.</div>
        )}
      </section>
    </div>
  );
}
