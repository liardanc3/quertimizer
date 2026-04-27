import { type KeyboardEvent, useEffect, useMemo, useState } from 'react';
import CommunityTiptapEditor from '../components/community/CommunityTiptapEditor';
import HttpErrorState from '../components/common/HttpErrorState';
import ContentLoading from '../components/common/LoadingSpinner';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import {
  clearCommunityEditorDraft,
  getCommunityEditorDraft,
  saveCommunityEditorDraft,
} from '../lib/communityStore';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import {
  createCommunityPost,
  fetchCommunityPostDetail,
  fetchCommunityTagSuggestions,
  updateCommunityPost,
  uploadCommunityImage,
  type CommunityTagSuggestion,
} from '../lib/communityApi';
import { COMMUNITY_POST_CONTENT_MAX_BYTES } from '../lib/communityContent';
import { type CommunityEditorSnapshot } from '../lib/communityTiptap';
import { COMMUNITY_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import { openLoginOverlay, setLoginOverlayDescription } from '../lib/authOverlay';
import { showSessionToast, useMockSession } from '../lib/session';
import { getUiTextValue, useUiText } from '../lib/uiText';
import type { CommunityPostCategory } from '../types/domain';
import './CommunityPage.css';

interface CommunityWritePageProps {
  postId?: string;
  embedded?: boolean;
}

interface EditorValues {
  title: string;
  category: EditableCommunityCategory;
  draftTag: string;
  selectedTags: string[];
  contentJson: string;
}

interface CommunityWriteFavoriteSnapshot extends EditorValues {
  postId?: string;
}

const POST_DRAFT_LOGIN_DESCRIPTION = getUiTextValue('COMMUNITY_WRITE_LOGIN_DRAFT_MESSAGE', '작성 중인 게시글은 유지됩니다. 로그인 후 이어서 작성할 수 있습니다.');
const contentByteFormatter = new Intl.NumberFormat('ko-KR');
type EditableCommunityCategory = Extract<CommunityPostCategory, 'discussion' | 'question' | 'notice'>;

const emptyEditorSnapshot: CommunityEditorSnapshot = {
  contentJson: '',
  plainTextSummary: '',
  imageIds: [],
  contentByteLength: 0,
  empty: true,
};

function normalizeKeyword(value: string) {
  return value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[_\-\s]+/g, '')
    .replace(/[^\p{L}\p{N}]/gu, '');
}

function TagRemoveIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.4 4.4 7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="m11.6 4.4-7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function CategoryArrowIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.8 6.4 3.2 3.2 3.2-3.2" stroke="currentColor" strokeWidth="1.55" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function getCategoryLabel(category: EditableCommunityCategory) {
  if (category === 'notice') {
    return getUiTextValue('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지');
  }

  if (category === 'question') {
    return getUiTextValue('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문');
  }

  return getUiTextValue('COMMUNITY_CATEGORY_FREE_LABEL', '자유');
}

function normalizeEditableCategory(category?: string): EditableCommunityCategory {
  if (category === 'notice' || category === 'question') {
    return category;
  }

  return 'discussion';
}

function readWriteCategoryFromSearch() {
  return normalizeEditableCategory(new URLSearchParams(window.location.search).get('category') ?? undefined);
}

function createCategoryOptions(isAdmin: boolean, selectedCategory: EditableCommunityCategory) {
  const options: EditableCommunityCategory[] = ['discussion', 'question'];

  if (isAdmin || selectedCategory === 'notice') {
    options.push('notice');
  }

  return options;
}

function createEmptyValues(category: EditableCommunityCategory): EditorValues {
  return {
    title: '',
    category,
    draftTag: '',
    selectedTags: [],
    contentJson: '',
  };
}

export default function CommunityWritePage({ postId, embedded = false }: CommunityWritePageProps) {
  const { text } = useUiText();
  const { isAuthenticated, isAdmin, isReady } = useMockSession();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<CommunityWriteFavoriteSnapshot>('communityWrite'), []);
  const draftKey = postId ? `community-edit-${postId}` : 'community-write';
  const initialCategory = useMemo(readWriteCategoryFromSearch, []);
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState<EditableCommunityCategory>(initialCategory);
  const [loadedCategory, setLoadedCategory] = useState<EditableCommunityCategory>(initialCategory);
  const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false);
  const [draftTag, setDraftTag] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [initialContentJson, setInitialContentJson] = useState('');
  const [editorSnapshot, setEditorSnapshot] = useState<CommunityEditorSnapshot>(emptyEditorSnapshot);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [tagSuggestions, setTagSuggestions] = useState<CommunityTagSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState(Boolean(postId));
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadErrorStatus, setLoadErrorStatus] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isHydrated, setIsHydrated] = useState(false);

  const normalizedDraftTag = normalizeKeyword(draftTag);
  const categoryOptions = createCategoryOptions(isAdmin, category);
  const hasPostDraft = title.trim() !== ''
    || category !== loadedCategory
    || draftTag.trim() !== ''
    || selectedTags.length > 0
    || !editorSnapshot.empty;

  useEffect(() => {
    setLoginOverlayDescription(hasPostDraft ? POST_DRAFT_LOGIN_DESCRIPTION : null);

    return () => setLoginOverlayDescription(null);
  }, [hasPostDraft]);

  useEffect(() => {
    clearFavoriteRestoreSnapshot('communityWrite');
  }, []);

  useEffect(() => {
    if (isReady && !isAdmin && !postId && category === 'notice') {
      setCategory('discussion');
      setLoadedCategory('discussion');
    }
  }, [category, isAdmin, isReady, postId]);

  useEffect(() => {
    if (!postId) {
      const savedDraft = getCommunityEditorDraft(draftKey);
      const favoriteDraft = favoriteRestoreSnapshot && (favoriteRestoreSnapshot.postId ?? null) === null
        ? {
            title: favoriteRestoreSnapshot.title,
            category: normalizeEditableCategory(favoriteRestoreSnapshot.category),
            draftTag: favoriteRestoreSnapshot.draftTag,
            selectedTags: favoriteRestoreSnapshot.selectedTags,
            contentJson: favoriteRestoreSnapshot.contentJson,
          }
        : null;
      const nextValues = favoriteDraft
        ?? (savedDraft
          ? {
              title: savedDraft.title,
              category: normalizeEditableCategory(savedDraft.category),
              draftTag: savedDraft.draftTag,
              selectedTags: savedDraft.selectedTags,
              contentJson: savedDraft.contentJson ?? '',
            }
          : createEmptyValues(initialCategory));

      setTitle(nextValues.title);
      setCategory(nextValues.category);
      setLoadedCategory(nextValues.category);
      setDraftTag(nextValues.draftTag);
      setSelectedTags(nextValues.selectedTags);
      setInitialContentJson(nextValues.contentJson);
      setIsHydrated(true);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setLoadFailed(false);
    setFeedback(null);
    setLoadErrorStatus(null);

    fetchCommunityPostDetail(postId)
      .then((post) => {
        if (cancelled) {
          return;
        }

        const savedDraft = getCommunityEditorDraft(draftKey);
        const baseValues: EditorValues = {
          title: post.title,
          category: normalizeEditableCategory(post.category),
          draftTag: '',
          selectedTags: post.tags,
          contentJson: post.contentJson,
        };
        const favoriteDraft = favoriteRestoreSnapshot && favoriteRestoreSnapshot.postId === postId
          ? {
              title: favoriteRestoreSnapshot.title,
              category: normalizeEditableCategory(favoriteRestoreSnapshot.category),
              draftTag: favoriteRestoreSnapshot.draftTag,
              selectedTags: favoriteRestoreSnapshot.selectedTags,
              contentJson: favoriteRestoreSnapshot.contentJson,
            }
          : null;
        const nextValues = favoriteDraft
          ?? (savedDraft
            ? {
                title: savedDraft.title,
                category: normalizeEditableCategory(savedDraft.category ?? baseValues.category),
                draftTag: savedDraft.draftTag,
                selectedTags: savedDraft.selectedTags,
                contentJson: savedDraft.contentJson ?? baseValues.contentJson,
              }
            : baseValues);

        setTitle(nextValues.title);
        setCategory(nextValues.category);
        setLoadedCategory(nextValues.category);
        setDraftTag(nextValues.draftTag);
        setSelectedTags(nextValues.selectedTags);
        setInitialContentJson(nextValues.contentJson);
        setIsHydrated(true);
        setLoadFailed(false);
        setLoadErrorStatus(null);
      })
      .catch((error) => {
        if (!cancelled) {
          setLoadFailed(true);
          setFeedback(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
          const status = getApiErrorStatus(error);
          setLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [draftKey, favoriteRestoreSnapshot, initialCategory, postId]);

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    if (!hasPostDraft) {
      clearCommunityEditorDraft(draftKey);
      return;
    }

    const timeoutId = window.setTimeout(() => {
      saveCommunityEditorDraft(draftKey, {
        title,
        category,
        draftTag,
        selectedTags,
        contentJson: editorSnapshot.contentJson,
      });
    }, 250);

    return () => window.clearTimeout(timeoutId);
  }, [category, draftKey, draftTag, editorSnapshot.contentJson, hasPostDraft, isHydrated, selectedTags, title]);

  useEffect(() => {
    if (!normalizedDraftTag || selectedTags.length >= 7) {
      setTagSuggestions([]);
      return;
    }

    let cancelled = false;

    fetchCommunityTagSuggestions(draftTag)
      .then((nextTagSuggestions) => {
        if (!cancelled) {
          setTagSuggestions(nextTagSuggestions);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTagSuggestions([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [draftTag, normalizedDraftTag, selectedTags.length]);

  function handleCancel() {
    if (window.history.state?.from) {
      window.history.back();
      return;
    }

    navigate(postId ? getCommunityPostPath(postId) : COMMUNITY_PATH);
  }

  function handleAddTag(tagLabel: string) {
    const trimmedTagLabel = tagLabel.trim().replace(/^#/, '');
    const normalizedLabel = normalizeKeyword(trimmedTagLabel);

    if (!normalizedLabel) {
      return;
    }

    if (selectedTags.length >= 7) {
      setFeedback(text('COMMUNITY_TAG_LIMIT_MESSAGE', '태그는 최대 7개까지 추가할 수 있습니다.'));
      return;
    }

    setSelectedTags((currentTags) => {
      if (currentTags.length >= 7) {
        return currentTags;
      }

      return currentTags.some((currentTag) => normalizeKeyword(currentTag) === normalizedLabel)
        ? currentTags
        : [...currentTags, trimmedTagLabel];
    });
    setDraftTag('');
    setFeedback(null);
  }

  function handleRemoveTag(tagLabel: string) {
    setSelectedTags((currentTags) => currentTags.filter((currentTag) => currentTag !== tagLabel));
  }

  function selectCategory(nextCategory: EditableCommunityCategory) {
    setCategory(nextCategory);
    setIsCategoryMenuOpen(false);
    setFeedback(null);
  }

  function renderCategorySelect() {
    return (
      <div className="community-category-select-wrap community-title-category-wrap">
        <button
          type="button"
          className="community-title-category-trigger"
          onClick={() => setIsCategoryMenuOpen((currentValue) => !currentValue)}
          aria-haspopup="menu"
          aria-expanded={isCategoryMenuOpen}
          aria-label={text('COMMUNITY_CATEGORY_SELECT_LABEL', '게시글 구분 선택')}
        >
          <span>{getCategoryLabel(category)}</span>
          <CategoryArrowIcon />
        </button>

        {isCategoryMenuOpen ? (
          <div className="community-category-select-menu community-title-category-menu" role="menu">
            {categoryOptions.map((option) => (
              <button
                key={option}
                type="button"
                className={`community-category-select-item ${option === category ? 'is-selected' : ''}`.trim()}
                onClick={() => selectCategory(option)}
                role="menuitem"
              >
                {getCategoryLabel(option)}
              </button>
            ))}
          </div>
        ) : null}
      </div>
    );
  }

  function handleTagKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      handleAddTag(draftTag);
    }
  }

  async function handleSubmit() {
    if (isSubmitting) {
      return;
    }

    if (!isAuthenticated) {
      openLoginOverlay(POST_DRAFT_LOGIN_DESCRIPTION);
      return;
    }

    if (!title.trim() || editorSnapshot.empty) {
      setFeedback(!title.trim() ? text('COMMUNITY_TITLE_REQUIRED_MESSAGE', '제목 입력은 필수입니다.') : text('COMMUNITY_BODY_REQUIRED_MESSAGE', '본문 입력은 필수입니다.'));
      return;
    }

    if (editorSnapshot.contentByteLength > COMMUNITY_POST_CONTENT_MAX_BYTES) {
      setFeedback(
        text(
          'COMMUNITY_CONTENT_MAX_BYTES_MESSAGE',
          { maxBytes: contentByteFormatter.format(COMMUNITY_POST_CONTENT_MAX_BYTES) },
          `본문은 최대 ${contentByteFormatter.format(COMMUNITY_POST_CONTENT_MAX_BYTES)} Byte까지 입력할 수 있습니다.`,
        ),
      );
      showSessionToast(text('COMMUNITY_UPLOAD_FAIL_TOAST', '업로드에 실패했습니다.'));
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        title: title.trim(),
        category,
        tags: selectedTags,
        contentJson: editorSnapshot.contentJson,
        plainTextSummary: editorSnapshot.plainTextSummary,
        imageIds: editorSnapshot.imageIds,
      };
      const savedPostId = postId ? postId : await createCommunityPost(payload);

      if (postId) {
        await updateCommunityPost(postId, payload);
      }

      clearCommunityEditorDraft(draftKey);
      showSessionToast(text('COMMUNITY_UPLOAD_SUCCESS_TOAST', '업로드했습니다.'));
      navigate(getCommunityPostPath(savedPostId), {
        state: {
          from: window.history.state?.from ?? COMMUNITY_PATH,
        },
      });
    } catch (error) {
      showSessionToast(text('COMMUNITY_UPLOAD_FAIL_TOAST', '업로드에 실패했습니다.'));
      setFeedback(error instanceof Error ? error.message : text('COMMUNITY_POST_SAVE_FAIL_MESSAGE', '게시글을 저장하지 못했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  const suggestedTags = useMemo(
    () => tagSuggestions
      .filter((tag) => !selectedTags.some((selectedTag) => normalizeKeyword(selectedTag) === normalizeKeyword(tag.tag)))
      .slice(0, 7),
    [selectedTags, tagSuggestions],
  );

  function renderWritePanel() {
    return (
      <>
        <section className="panel-card community-detail-card community-write-page">
          <div className="community-detail-header community-write-edit-header">
            <div className="community-title-input-row">
              {renderCategorySelect()}

              <input
                type="text"
                value={title}
                onChange={(event) => {
                  setTitle(event.target.value);
                  setFeedback(null);
                }}
                onFocus={() => setIsCategoryMenuOpen(false)}
                className="text-field community-detail-title-input"
                placeholder={text('COMMUNITY_TITLE_PLACEHOLDER', '제목')}
              />
            </div>

            <div className="community-detail-edit-tags community-write-edit-tags">
              {selectedTags.length > 0 ? (
                <div className="community-detail-edit-tag-list">
                  {selectedTags.map((tag) => (
                    <button key={tag} type="button" className="community-detail-edit-tag" onClick={() => handleRemoveTag(tag)}>
                      <span>#{tag}</span>
                      <span aria-hidden="true" className="community-detail-edit-tag-remove">
                        <TagRemoveIcon />
                      </span>
                    </button>
                  ))}
                </div>
              ) : null}

              <input
                type="text"
                value={draftTag}
                onChange={(event) => {
                  setDraftTag(event.target.value);
                  setFeedback(null);
                }}
                onKeyDown={handleTagKeyDown}
                className="text-field community-detail-edit-tag-input"
                placeholder={selectedTags.length >= 7 ? text('COMMUNITY_TAG_LIMIT_PLACEHOLDER', '태그는 최대 7개') : text('COMMUNITY_TAG_PLACEHOLDER', '태그 추가')}
              />

              {draftTag.trim() && selectedTags.length < 7 && suggestedTags.length > 0 ? (
                <div className="community-detail-edit-tag-list community-write-suggested-tag-list">
                  {suggestedTags.map((tag) => (
                    <button key={tag.tag} type="button" className="community-detail-edit-tag" onClick={() => handleAddTag(tag.tag)}>
                      <span>#{tag.tag}</span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="community-content-body">
              <CommunityTiptapEditor
                initialContentJson={initialContentJson}
                placeholder=""
                onSnapshot={(snapshot) => {
                  setEditorSnapshot(snapshot);
                  setFeedback(null);
                }}
                onUploadImage={uploadCommunityImage}
                onFeedback={setFeedback}
              />

              <div className={`community-editor-byte-indicator ${editorSnapshot.contentByteLength > COMMUNITY_POST_CONTENT_MAX_BYTES ? 'is-over' : ''}`.trim()}>
                {contentByteFormatter.format(editorSnapshot.contentByteLength)} / {contentByteFormatter.format(COMMUNITY_POST_CONTENT_MAX_BYTES)} Byte
              </div>

              <div className="community-write-text-actions">
                <button type="button" className="community-write-text-action is-cancel" onClick={handleCancel}>
                  {text('COMMON_CANCEL_BUTTON', '취소')}
                </button>
                <button
                  type="button"
                  className="community-write-text-action is-submit"
                  onClick={() => void handleSubmit()}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? text('COMMUNITY_UPLOADING_BUTTON', '업로드 중') : text('COMMUNITY_UPLOAD_BUTTON', '업로드')}
                </button>
              </div>
            </div>
          </div>
        </section>

        {feedback ? (
          <section className="panel-card compact community-feedback-card">
            <p className="community-feedback-text">{feedback}</p>
          </section>
        ) : null}
      </>
    );
  }

  if (isLoading) {
    const loadingPanel = (
      <section className="panel-card community-detail-card community-write-page">
        <ContentLoading className="community-write-loading" />
      </section>
    );

    return embedded ? (
      <div className="community-detail-page community-write-root community-write-embedded-root">{loadingPanel}</div>
    ) : (
      <div className="page-stack community-detail-page community-write-root">{loadingPanel}</div>
    );
  }

  if (postId && loadFailed) {
    const notFoundPanel = (
      <section className="panel-card community-detail-card community-write-page">
        <div className="community-detail-header">
          {loadErrorStatus != null ? <HttpErrorState status={loadErrorStatus} message={feedback} /> : <PageLoadFailureState message={feedback} />}
        </div>
      </section>
    );

    return embedded ? (
      <div className="community-detail-page community-write-root community-write-embedded-root">{notFoundPanel}</div>
    ) : (
      <div className="page-stack community-detail-page community-write-root">{notFoundPanel}</div>
    );
  }

  return embedded ? (
    <div className="community-detail-page community-write-root community-write-embedded-root">{renderWritePanel()}</div>
  ) : (
    <div className="page-stack community-detail-page community-write-root">{renderWritePanel()}</div>
  );
}
