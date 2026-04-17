import { useEffect, useMemo, useRef, useState } from 'react';
import { getProfileActivityPath, getProfilePath, navigate } from '../lib/navigation';
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
import { syncSession, useMockSession } from '../lib/session';
import type { DbmsType } from '../types/domain';
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

type RecordSortField = 'problemId' | 'executionTimeMs' | 'submittedAt';
type SortDirection = 'asc' | 'desc';

const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];

const emptySolvedProblems: UserProfileSolvedProblems = {
  solvedProblemCount: 0,
  solvedProblemIds: [],
};

const emptySolvedRecords: UserProfileSolvedRecords = {
  solvedRecords: [],
};

const numberFormatter = new Intl.NumberFormat('ko-KR');

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

function formatPercentile(value: number | null) {
  if (value == null) {
    return '-';
  }

  return `${Math.round(value * 10) / 10}%`;
}

function formatRecordDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}.${month}.${day} ${hours}:${minutes}`;
}

function normalizeLinksForSave(links: UserProfileLink[]) {
  return links
    .map((link) => ({
      type: link.type.trim(),
      value: link.value.trim(),
    }))
    .filter((link) => link.type !== '' && link.value !== '');
}

function getNextSortDirection(currentDirection?: SortDirection) {
  if (currentDirection == null) {
    return 'asc';
  }

  if (currentDirection === 'asc') {
    return 'desc';
  }

  return null;
}

function compareRecords(field: RecordSortField, direction: SortDirection, left: UserProfileSolvedRecord, right: UserProfileSolvedRecord) {
  let comparedValue = 0;

  if (field === 'problemId') {
    comparedValue = left.problemId.localeCompare(right.problemId);
  }

  if (field === 'executionTimeMs') {
    comparedValue = left.executionTimeMs - right.executionTimeMs;
  }

  if (field === 'submittedAt') {
    comparedValue = new Date(left.submittedAt).getTime() - new Date(right.submittedAt).getTime();
  }

  return direction === 'asc' ? comparedValue : comparedValue * -1;
}

function SortIcon({ direction }: { direction?: SortDirection }) {
  return <span aria-hidden="true">{direction === 'asc' ? '↑' : direction === 'desc' ? '↓' : '↕'}</span>;
}

function FilterIcon({ isActive }: { isActive: boolean }) {
  return <span aria-hidden="true">{isActive ? '●' : '◌'}</span>;
}

function EmptyProfileState() {
  return (
    <div className="page-stack profile-page">
      <section className="panel-card">
        <p className="panel-meta">프로필</p>
        <h1 className="page-title">조회할 프로필이 없다.</h1>
        <p className="muted-text">공개 프로필은 <code>/profile/{'{userId}'}</code> 경로로 조회할 수 있다.</p>
      </section>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="page-stack profile-page">
      <section className="panel-card">
        <p className="panel-meta">프로필</p>
        <h1 className="page-title">프로필을 불러오는 중...</h1>
      </section>
    </div>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <div className="page-stack profile-page">
      <section className="panel-card">
        <p className="panel-meta">프로필</p>
        <h1 className="page-title">프로필을 불러오지 못했다.</h1>
        <p className="muted-text">{message}</p>
      </section>
    </div>
  );
}

export default function ProfilePage({ handle }: ProfilePageProps) {
  const { isAuthenticated, isReady, userId } = useMockSession();
  const [profileSummary, setProfileSummary] = useState<UserProfileSummary | null>(null);
  const [solvedProblems, setSolvedProblems] = useState<UserProfileSolvedProblems>(emptySolvedProblems);
  const [solvedRecords, setSolvedRecords] = useState<UserProfileSolvedRecords>(emptySolvedRecords);
  const [isSummaryLoading, setIsSummaryLoading] = useState(true);
  const [isSolvedProblemsLoading, setIsSolvedProblemsLoading] = useState(true);
  const [isSolvedRecordsLoading, setIsSolvedRecordsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editDraft, setEditDraft] = useState<ProfileEditDraft | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [recordSortField, setRecordSortField] = useState<RecordSortField | null>(null);
  const [recordSortDirection, setRecordSortDirection] = useState<SortDirection | null>(null);
  const [visibleDbmsFilters, setVisibleDbmsFilters] = useState<DbmsType[]>(['postgresql', 'oracle']);
  const [isDbmsFilterOpen, setIsDbmsFilterOpen] = useState(false);
  const dbmsFilterRef = useRef<HTMLDivElement | null>(null);

  const resolvedProfileId = handle ?? userId;
  const isOwnProfile = isAuthenticated && userId != null && resolvedProfileId === userId;
  const visibleLinks = profileSummary?.links ?? [];
  const solvedProblemIds = useMemo(() => solvedProblems.solvedProblemIds, [solvedProblems]);
  const showExecutionPercentiles = isOwnProfile || profileSummary?.executionPercentilePublic === true;
  const showSolvedProblemSection = isOwnProfile || profileSummary?.solvedProblemCountPublic === true;
  const showSolvedRecordsSection = isOwnProfile || profileSummary?.solvedRecordsPublic === true;
  const visibleSolvedRecords = useMemo(() => {
    const filteredRecords = solvedRecords.solvedRecords.filter((record) => visibleDbmsFilters.includes(record.dbms));

    if (recordSortField == null || recordSortDirection == null) {
      return filteredRecords;
    }

    return [...filteredRecords].sort((left, right) => compareRecords(recordSortField, recordSortDirection, left, right));
  }, [recordSortDirection, recordSortField, solvedRecords, visibleDbmsFilters]);

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!resolvedProfileId) {
      setProfileSummary(null);
      setSolvedProblems(emptySolvedProblems);
      setSolvedRecords(emptySolvedRecords);
      setIsSummaryLoading(false);
      setIsSolvedProblemsLoading(false);
      setIsSolvedRecordsLoading(false);
      setErrorMessage(null);
      return;
    }

    let cancelled = false;

    setIsSummaryLoading(true);
    setIsSolvedProblemsLoading(true);
    setIsSolvedRecordsLoading(true);
    setErrorMessage(null);
    setFeedback(null);
    setSolvedProblems(emptySolvedProblems);
    setSolvedRecords(emptySolvedRecords);

    const loadProfileSummary = isOwnProfile ? fetchMyProfileSummary() : fetchProfileSummary(resolvedProfileId);
    const loadSolvedProblems = isOwnProfile ? fetchMySolvedProblems() : fetchSolvedProblems(resolvedProfileId);
    const loadSolvedRecords = isOwnProfile ? fetchMySolvedRecords() : fetchSolvedRecords(resolvedProfileId);

    loadProfileSummary
      .then((nextProfileSummary: UserProfileSummary) => {
        if (cancelled) {
          return;
        }

        setProfileSummary(nextProfileSummary);
        setEditDraft(createEditDraft(nextProfileSummary));
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setProfileSummary(null);
        setEditDraft(null);
        setErrorMessage(error instanceof Error ? error.message : '프로필 조회에 실패했다.');
      })
      .finally(() => {
        if (!cancelled) {
          setIsSummaryLoading(false);
        }
      });

    loadSolvedProblems
      .then((nextSolvedProblems) => {
        if (!cancelled) {
          setSolvedProblems(nextSolvedProblems);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSolvedProblems(emptySolvedProblems);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsSolvedProblemsLoading(false);
        }
      });

    loadSolvedRecords
      .then((nextSolvedRecords) => {
        if (!cancelled) {
          setSolvedRecords(nextSolvedRecords);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSolvedRecords(emptySolvedRecords);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsSolvedRecordsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isOwnProfile, isReady, resolvedProfileId]);

  useEffect(() => {
    if (!isDbmsFilterOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      if (dbmsFilterRef.current?.contains(event.target as Node)) {
        return;
      }

      setIsDbmsFilterOpen(false);
    }

    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [isDbmsFilterOpen]);

  if (!isReady || isSummaryLoading) {
    return <LoadingState />;
  }

  if (!resolvedProfileId) {
    return <EmptyProfileState />;
  }

  if (!profileSummary) {
    return <ErrorState message={errorMessage ?? '프로필을 찾을 수 없다.'} />;
  }

  function updateDraft(updater: (draft: ProfileEditDraft) => ProfileEditDraft) {
    setEditDraft((currentDraft) => (currentDraft ? updater(currentDraft) : currentDraft));
  }

  function toggleRecordSort(field: RecordSortField) {
    if (recordSortField !== field) {
      setRecordSortField(field);
      setRecordSortDirection('asc');
      return;
    }

    const nextDirection = getNextSortDirection(recordSortDirection ?? undefined);

    if (nextDirection == null) {
      setRecordSortField(null);
      setRecordSortDirection(null);
      return;
    }

    setRecordSortDirection(nextDirection);
  }

  function toggleDbmsFilter(dbms: DbmsType) {
    setVisibleDbmsFilters((currentFilters) => {
      if (currentFilters.includes(dbms)) {
        if (currentFilters.length === 1) {
          return currentFilters;
        }

        return currentFilters.filter((currentDbms) => currentDbms !== dbms);
      }

      return [...currentFilters, dbms];
    });
  }

  async function saveProfile() {
    if (!editDraft) {
      return;
    }

    const normalizedLinks = normalizeLinksForSave(editDraft.links);
    const payload: UpdateUserProfilePayload = {
      bio: editDraft.bio,
      links: normalizedLinks,
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
      setFeedback({ tone: 'success', message: '프로필을 저장했다.' });
      void syncSession();
    } catch (error) {
      setFeedback({
        tone: 'error',
        message: error instanceof Error ? error.message : '프로필 저장에 실패했다.',
      });
    }
  }

  return (
    <div className="page-stack profile-page">
      <section className="panel-card profile-hero-card">
        <div className="profile-hero-layout profile-hero-layout-simple">
          <div className="profile-hero-copy">
            <div className="profile-hero-meta-row">
              <p className="panel-meta">프로필</p>
              <span className="subtle-chip">{isOwnProfile ? '내 프로필' : '공개 프로필'}</span>
            </div>

            <h1 className="page-title">@{profileSummary.userId}</h1>
            <p className="profile-bio-text">{profileSummary.bio || '소개글이 없다.'}</p>

            {visibleLinks.length > 0 ? (
              <div className="profile-link-list">
                {visibleLinks.map((link, index) => (
                  <div key={`${link.type}-${link.value}-${index}`} className="profile-link-chip">
                    <span className="profile-link-label">{link.type}</span>
                    <span className="profile-link-value">{link.value}</span>
                  </div>
                ))}
              </div>
            ) : null}
          </div>

          {isOwnProfile ? (
            <div className="profile-hero-actions">
              <button
                type="button"
                className="btn secondary"
                onClick={() => {
                  setEditDraft(createEditDraft(profileSummary));
                  setFeedback(null);
                  setIsEditOpen((value) => !value);
                }}
              >
                {isEditOpen ? '편집 닫기' : '프로필 편집'}
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

      {showExecutionPercentiles ? (
        <section className="panel-card compact">
          <div className="profile-summary-grid">
            <article className="profile-summary-card">
              <p className="stat-label">평균 실행시간 백분위 (PostgreSQL)</p>
              <p className="profile-summary-value">{formatPercentile(profileSummary.averageExecutionPercentilePostgresql)}</p>
            </article>
            <article className="profile-summary-card">
              <p className="stat-label">평균 실행시간 백분위 (Oracle)</p>
              <p className="profile-summary-value">{formatPercentile(profileSummary.averageExecutionPercentileOracle)}</p>
            </article>
          </div>
        </section>
      ) : null}

      <section className="panel-card compact">
        <div className="profile-summary-grid">
          <button
            type="button"
            className="profile-summary-card profile-activity-card"
            onClick={() => navigate(getProfileActivityPath(handle, 'posts'))}
          >
            <p className="stat-label">작성한 게시글</p>
            <p className="profile-summary-value">{numberFormatter.format(profileSummary.authoredPostCount)}</p>
          </button>
          <button
            type="button"
            className="profile-summary-card profile-activity-card"
            onClick={() => navigate(getProfileActivityPath(handle, 'likes'))}
          >
            <p className="stat-label">좋아요 누른 글</p>
            <p className="profile-summary-value">{numberFormatter.format(profileSummary.likedPostCount)}</p>
          </button>
          <button
            type="button"
            className="profile-summary-card profile-activity-card"
            onClick={() => navigate(getProfileActivityPath(handle, 'comments'))}
          >
            <p className="stat-label">작성한 댓글</p>
            <p className="profile-summary-value">{numberFormatter.format(profileSummary.commentCount)}</p>
          </button>
        </div>
      </section>

      {isOwnProfile && isEditOpen && editDraft ? (
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <span className="subtle-chip">링크 최대 10개</span>
          </div>

          <div className="profile-settings-stack">
            <label className="field-stack">
              <span className="field-label">소개글</span>
              <textarea
                className="text-field profile-textarea"
                value={editDraft.bio}
                onChange={(event) =>
                  updateDraft((draft) => ({
                    ...draft,
                    bio: event.target.value,
                  }))
                }
                placeholder="소개글을 입력해라."
              />
            </label>

            <div className="profile-settings-group">
              <div className="panel-heading-row responsive">
                <div className="profile-link-heading">
                  <p className="field-label">프로필 링크</p>
                  <button
                    type="button"
                    className="profile-link-add-button"
                    aria-label="프로필 링크 추가"
                    disabled={editDraft.links.length >= 10}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        links: [...draft.links, { type: '', value: '' }],
                      }))
                    }
                  >
                    +
                  </button>
                </div>
              </div>

              <div className="profile-link-editor-list">
                {editDraft.links.map((link, index) => (
                  <div key={`profile-link-${index}`} className="profile-link-editor-row">
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
                              : currentLink
                          ),
                        }))
                      }
                      placeholder="blog"
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
                              : currentLink
                          ),
                        }))
                      }
                      placeholder="github.com/user"
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
            </div>

            <div className="profile-inline-settings">
              <div className="profile-inline-setting">
                <p className="field-label">선호 DBMS</p>
              <div className="segmented profile-settings-segmented">
                {dbmsOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`segmented-btn ${editDraft.defaultDbms === option.value ? 'is-selected' : ''}`}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        defaultDbms: option.value,
                      }))
                    }
                  >
                    {option.label}
                  </button>
                ))}
              </div>
              </div>
              <div className="profile-inline-setting">
                <p className="field-label">실행시간 백분위</p>
                <div className="segmented profile-settings-segmented">
                  <button
                    type="button"
                    className={`segmented-btn ${editDraft.executionPercentilePublic ? 'is-selected' : ''}`}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        executionPercentilePublic: true,
                      }))
                    }
                  >
                    공개
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${!editDraft.executionPercentilePublic ? 'is-selected' : ''}`}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        executionPercentilePublic: false,
                      }))
                    }
                  >
                    비공개
                  </button>
                </div>
              </div>
              <div className="profile-inline-setting">
                <p className="field-label">풀이 기록</p>
                <div className="segmented profile-settings-segmented">
                  <button
                    type="button"
                    className={`segmented-btn ${editDraft.solvedRecordsPublic ? 'is-selected' : ''}`}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        solvedRecordsPublic: true,
                      }))
                    }
                  >
                    공개
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${!editDraft.solvedRecordsPublic ? 'is-selected' : ''}`}
                    onClick={() =>
                      updateDraft((draft) => ({
                        ...draft,
                        solvedRecordsPublic: false,
                      }))
                    }
                  >
                    비공개
                  </button>
                </div>
              </div>
            </div>

            <div className="profile-settings-group is-hidden">
              <div>
                <p className="field-label">SQL 공개 설정</p>
                <p className="hint-text">프로필에 SQL 기록 공개 여부를 저장한다.</p>
              </div>
              <div className="segmented profile-settings-segmented">
                <button
                  type="button"
                  className={`segmented-btn ${editDraft.sqlPublic ? 'is-selected' : ''}`}
                  onClick={() =>
                    updateDraft((draft) => ({
                      ...draft,
                      sqlPublic: true,
                    }))
                  }
                >
                  공개
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${!editDraft.sqlPublic ? 'is-selected' : ''}`}
                  onClick={() =>
                    updateDraft((draft) => ({
                      ...draft,
                      sqlPublic: false,
                    }))
                  }
                >
                  비공개
                </button>
              </div>
            </div>

            <div className="profile-settings-group is-hidden">
              <div>
                <p className="field-label">프로필 노출 설정</p>
                <p className="hint-text">공개 프로필에서 어떤 기록을 보여줄지 저장한다.</p>
              </div>

              <div className="profile-visibility-stack">
                <div className="profile-visibility-item">
                  <p className="field-label">실행시간 백분위</p>
                  <div className="segmented profile-settings-segmented">
                    <button
                      type="button"
                      className={`segmented-btn ${editDraft.executionPercentilePublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          executionPercentilePublic: true,
                        }))
                      }
                    >
                      공개
                    </button>
                    <button
                      type="button"
                      className={`segmented-btn ${!editDraft.executionPercentilePublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          executionPercentilePublic: false,
                        }))
                      }
                    >
                      비공개
                    </button>
                  </div>
                </div>

                <div className="profile-visibility-item">
                  <p className="field-label">풀이 기록</p>
                  <div className="segmented profile-settings-segmented">
                    <button
                      type="button"
                      className={`segmented-btn ${editDraft.solvedRecordsPublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          solvedRecordsPublic: true,
                        }))
                      }
                    >
                      공개
                    </button>
                    <button
                      type="button"
                      className={`segmented-btn ${!editDraft.solvedRecordsPublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          solvedRecordsPublic: false,
                        }))
                      }
                    >
                      비공개
                    </button>
                  </div>
                </div>

                <div className="profile-visibility-item">
                  <p className="field-label">푼 문제 수</p>
                  <div className="segmented profile-settings-segmented">
                    <button
                      type="button"
                      className={`segmented-btn ${editDraft.solvedProblemCountPublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          solvedProblemCountPublic: true,
                        }))
                      }
                    >
                      공개
                    </button>
                    <button
                      type="button"
                      className={`segmented-btn ${!editDraft.solvedProblemCountPublic ? 'is-selected' : ''}`}
                      onClick={() =>
                        updateDraft((draft) => ({
                          ...draft,
                          solvedProblemCountPublic: false,
                        }))
                      }
                    >
                      비공개
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="auth-actions">
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
        </section>
      ) : null}

      {showSolvedProblemSection ? (
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <p className="panel-meta">해결한 문제</p>
            <span className="subtle-chip">문제 {numberFormatter.format(solvedProblems.solvedProblemCount)}개</span>
          </div>

          {isSolvedProblemsLoading ? (
            <div className="profile-empty-state">불러오는 중이다.</div>
          ) : solvedProblemIds.length > 0 ? (
            <div className="profile-problem-chip-list">
              {solvedProblemIds.map((problemId) => (
                <button
                  key={problemId}
                  type="button"
                  className="mini-toggle profile-problem-chip"
                  onClick={() => navigate(`/problems/${problemId}`)}
                >
                  {problemId}
                </button>
              ))}
            </div>
          ) : (
            <div className="profile-empty-state">아직 해결한 문제가 없다.</div>
          )}
        </section>
      ) : null}

      {showSolvedRecordsSection ? (
        <section className="panel-card">
          <div className="panel-heading-row responsive">
            <p className="panel-meta">해결 기록</p>
          </div>

          {isSolvedRecordsLoading ? (
            <div className="profile-empty-state">불러오는 중이다.</div>
          ) : visibleSolvedRecords.length > 0 ? (
            <div className="profile-records-shell">
              <div className="profile-records-scroll">
                <table className="profile-records-table">
                  <thead>
                    <tr>
                      <th>
                        <div className="profile-record-header">
                          <span>문제 번호</span>
                          <button
                            type="button"
                            className={`profile-record-icon-button ${recordSortField === 'problemId' ? 'is-active' : ''}`}
                            onClick={() => toggleRecordSort('problemId')}
                            aria-label="문제 번호 정렬"
                          >
                            <SortIcon direction={recordSortField === 'problemId' ? recordSortDirection ?? undefined : undefined} />
                          </button>
                        </div>
                      </th>
                      <th>
                        <div className="profile-record-header" ref={dbmsFilterRef}>
                          <span>DBMS</span>
                          <button
                            type="button"
                            className={`profile-record-icon-button ${visibleDbmsFilters.length !== dbmsOptions.length ? 'is-active' : ''}`}
                            onClick={() => setIsDbmsFilterOpen((value) => !value)}
                            aria-label="DBMS 필터"
                          >
                            <FilterIcon isActive={visibleDbmsFilters.length !== dbmsOptions.length} />
                          </button>

                          {isDbmsFilterOpen ? (
                            <div className="profile-record-filter-popover">
                              {dbmsOptions.map((option) => (
                                <label key={option.value} className="profile-record-filter-option">
                                  <input
                                    type="checkbox"
                                    checked={visibleDbmsFilters.includes(option.value)}
                                    onChange={() => toggleDbmsFilter(option.value)}
                                  />
                                  <span>{option.label}</span>
                                </label>
                              ))}
                            </div>
                          ) : null}
                        </div>
                      </th>
                      <th>
                        <div className="profile-record-header">
                          <span>실행 시간</span>
                          <button
                            type="button"
                            className={`profile-record-icon-button ${recordSortField === 'executionTimeMs' ? 'is-active' : ''}`}
                            onClick={() => toggleRecordSort('executionTimeMs')}
                            aria-label="실행 시간 정렬"
                          >
                            <SortIcon direction={recordSortField === 'executionTimeMs' ? recordSortDirection ?? undefined : undefined} />
                          </button>
                        </div>
                      </th>
                      <th>
                        <div className="profile-record-header">
                          <span>제출 시각</span>
                          <button
                            type="button"
                            className={`profile-record-icon-button ${recordSortField === 'submittedAt' ? 'is-active' : ''}`}
                            onClick={() => toggleRecordSort('submittedAt')}
                            aria-label="제출 시각 정렬"
                          >
                            <SortIcon direction={recordSortField === 'submittedAt' ? recordSortDirection ?? undefined : undefined} />
                          </button>
                        </div>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleSolvedRecords.map((record) => (
                      <tr key={`${record.problemId}-${record.dbms}-${record.submittedAt}`}>
                        <td>
                          <button
                            type="button"
                            className="btn text inline profile-record-link"
                            onClick={() => navigate(`/problems/${record.problemId}`)}
                          >
                            <span className="profile-record-problem-id">{record.problemId}</span>
                            <span className="profile-record-title">{record.problemTitle}</span>
                          </button>
                        </td>
                        <td>
                          <span className="profile-record-dbms-text">{record.dbms === 'oracle' ? 'Oracle' : 'PostgreSQL'}</span>
                        </td>
                        <td>
                          <span className="profile-record-execution-time">{`${Math.round(record.executionTimeMs * 10) / 10} ms`}</span>
                        </td>
                        <td>
                          <span className="profile-record-date">{formatRecordDate(record.submittedAt)}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <div className="profile-empty-state">표시할 해결 기록이 없다.</div>
          )}
        </section>
      ) : null}

      {!isOwnProfile && isAuthenticated && userId ? (
        <section className="panel-card compact">
          <button type="button" className="btn ghost" onClick={() => navigate(getProfilePath())}>
            내 프로필로 이동
          </button>
        </section>
      ) : null}
    </div>
  );
}
