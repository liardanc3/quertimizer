import { useEffect, useMemo, useState, useSyncExternalStore, type KeyboardEvent as ReactKeyboardEvent, type MouseEvent } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import CommunityCommentThread from '../components/community/CommunityCommentThread';
import CommunityTiptapEditor from '../components/community/CommunityTiptapEditor';
import CommunityTiptapViewer from '../components/community/CommunityTiptapViewer';
import {
  addCommunityComment,
  deleteCommunityPost,
  fetchCommunityPostDetail,
  toggleCommunityCommentLike,
  toggleCommunityPostLike,
  updateCommunityPost,
  uploadCommunityImage,
  type CommunityPostDetail,
} from '../lib/communityApi';
import { COMMUNITY_POST_CONTENT_MAX_BYTES } from '../lib/communityContent';
import { createCommunityEditorSnapshotFromJson, type CommunityEditorSnapshot } from '../lib/communityTiptap';
import { COMMUNITY_PATH, getProfilePath, PROBLEMS_PATH, navigate } from '../lib/navigation';
import { openLoginOverlay, setLoginOverlayDescription } from '../lib/authOverlay';
import { showSessionToast, useMockSession } from '../lib/session';
import './CommunityPage.css';

interface CommunityDetailPageProps {
  postId: string;
}

function subscribeLocationHash(callback: () => void) {
  window.addEventListener('popstate', callback);
  window.addEventListener('hashchange', callback);

  return () => {
    window.removeEventListener('popstate', callback);
    window.removeEventListener('hashchange', callback);
  };
}

function getLocationHashSnapshot() {
  return window.location.hash;
}

const numberFormatter = new Intl.NumberFormat('ko-KR');
const COMMENT_LOGIN_DESCRIPTION = '작성 중인 댓글은 유지됩니다. 로그인 후 이어서 작성할 수 있습니다.';
type EditableCommunityCategory = Extract<CommunityPostDetail['category'], 'discussion' | 'question' | 'notice'>;

const emptyEditorSnapshot: CommunityEditorSnapshot = {
  contentJson: '',
  plainTextSummary: '',
  imageIds: [],
  contentByteLength: 0,
  empty: true,
};

function formatBoardDate(value: string) {
  const date = new Date(value);
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function isProblemTag(tag: string) {
  return /^[PO]?\d{5}-\d{5}$/.test(tag.trim());
}

function getCategoryLabel(value: CommunityPostDetail['category']) {
  if (value === 'question') {
    return '질문';
  }

  if (value === 'notice') {
    return '공지';
  }

  if (value === 'tip') {
    return '팁';
  }

  return '자유';
}

function CategoryArrowIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.8 6.4 3.2 3.2 3.2-3.2" stroke="currentColor" strokeWidth="1.55" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function normalizeEditableCategory(category?: string): EditableCommunityCategory {
  if (category === 'notice' || category === 'question') {
    return category;
  }

  return 'discussion';
}

function createCategoryOptions(isAdmin: boolean, selectedCategory: EditableCommunityCategory) {
  const options: EditableCommunityCategory[] = ['discussion', 'question'];

  if (isAdmin || selectedCategory === 'notice') {
    options.push('notice');
  }

  return options;
}

function normalizeTag(value: string) {
  return value.trim().replace(/^#/, '');
}

function LinkCopyIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <rect x="5.2" y="3.2" width="6.6" height="8.1" rx="1.1" stroke="currentColor" strokeWidth="1.28" />
      <path d="M4.2 5V11a1.3 1.3 0 0 0 1.3 1.3h4.9" stroke="currentColor" strokeWidth="1.28" strokeLinecap="round" />
    </svg>
  );
}

function EditIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m10.9 2.6 2.5 2.5-7.4 7.4-3 .6.6-3 7.3-7.5Z" stroke="currentColor" strokeWidth="1.28" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function DeleteIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.5 4.4h9" stroke="currentColor" strokeWidth="1.28" strokeLinecap="round" />
      <path d="M6.1 4.4V3.2h3.8v1.2" stroke="currentColor" strokeWidth="1.28" strokeLinecap="round" strokeLinejoin="round" />
      <path d="m5.1 4.4.5 8h4.8l.5-8" stroke="currentColor" strokeWidth="1.28" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function LikeIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 13.3 3.5 9.1a2.8 2.8 0 0 1 4-4L8 5.6l.5-.5a2.8 2.8 0 0 1 4 4L8 13.3Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ViewIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2.4 8s1.9-3.7 5.6-3.7S13.6 8 13.6 8 11.7 11.7 8 11.7 2.4 8 2.4 8Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 9.75A1.75 1.75 0 1 0 8 6.25a1.75 1.75 0 0 0 0 3.5Z" stroke="currentColor" strokeWidth="1.35" />
    </svg>
  );
}

function CommentIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.5 3.9h9v6.1H7.4l-3.1 2.35V10h-.8V3.9Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2.7 7.6 13.3 3 9.4 13l-2.2-3.2-4.5-2.2Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function TagRemoveIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.4 4.4 7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="m11.6 4.4-7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function CancelEditIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.2 4.2 7.6 7.6" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" />
      <path d="m11.8 4.2-7.6 7.6" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" />
    </svg>
  );
}

function SaveEditIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m3.4 8.5 3 3 6.2-6.4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function CommunityDetailPage({ postId }: CommunityDetailPageProps) {
  const { isAuthenticated, isAdmin } = useMockSession();
  const locationHash = useSyncExternalStore(subscribeLocationHash, getLocationHashSnapshot, () => '');
  const [post, setPost] = useState<CommunityPostDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [commentDraft, setCommentDraft] = useState('');
  const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({});
  const [activeReplyId, setActiveReplyId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [lightboxImage, setLightboxImage] = useState<{ src: string; alt: string } | null>(null);
  const [hoveredTag, setHoveredTag] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isSavingEdit, setIsSavingEdit] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editCategory, setEditCategory] = useState<EditableCommunityCategory>('discussion');
  const [isEditCategoryMenuOpen, setIsEditCategoryMenuOpen] = useState(false);
  const [editDraftTag, setEditDraftTag] = useState('');
  const [editTags, setEditTags] = useState<string[]>([]);
  const [editInitialContentJson, setEditInitialContentJson] = useState('');
  const [editSnapshot, setEditSnapshot] = useState<CommunityEditorSnapshot>(emptyEditorSnapshot);
  const hasCommentDraft = commentDraft.trim() !== '' || Object.values(replyDrafts).some((replyDraft) => replyDraft.trim() !== '');
  const editCategoryOptions = createCategoryOptions(isAdmin, editCategory);

  useEffect(() => {
    setLoginOverlayDescription(hasCommentDraft ? COMMENT_LOGIN_DESCRIPTION : null);

    return () => setLoginOverlayDescription(null);
  }, [hasCommentDraft]);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);

    fetchCommunityPostDetail(postId)
      .then((nextPost) => {
        if (cancelled) {
          return;
        }

        setPost(nextPost);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }

        setPost(null);
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [postId]);

  useEffect(() => {
    if (!lightboxImage) {
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setLightboxImage(null);
      }
    }

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [lightboxImage]);

  useEffect(() => {
    if (!post || !locationHash.startsWith('#')) {
      return;
    }

    const targetId = decodeURIComponent(locationHash.slice(1));
    if (targetId === '') {
      return;
    }

    let clearHighlightTimerId = 0;
    const timerId = window.setTimeout(() => {
      const resolvedTargetId =
        document.getElementById(targetId) != null
          ? targetId
          : targetId.startsWith('comment-')
            ? `community-${targetId}`
            : targetId.startsWith('community-comment-')
              ? targetId
              : `community-comment-${targetId}`;
      const targetElement = document.getElementById(resolvedTargetId);

      if (!targetElement) {
        return;
      }

      targetElement.classList.remove('is-hash-target');
      void targetElement.getBoundingClientRect();
      targetElement.classList.add('is-hash-target');
      targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      targetElement.focus({ preventScroll: true });
      clearHighlightTimerId = window.setTimeout(() => {
        targetElement.classList.remove('is-hash-target');
      }, 10000);
    }, 0);

    return () => {
      window.clearTimeout(timerId);
      window.clearTimeout(clearHighlightTimerId);
    };
  }, [locationHash, post]);

  async function reloadPost() {
    try {
      const nextPost = await fetchCommunityPostDetail(postId);
      setPost(nextPost);
      return true;
    } catch {
      setFeedback('게시글을 새로고침하지 못했다.');
      return false;
    }
  }

  function openEditMode() {
    if (!post) {
      return;
    }

    setEditTitle(post.title);
    setEditCategory(normalizeEditableCategory(post.category));
    setIsEditCategoryMenuOpen(false);
    setEditDraftTag('');
    setEditTags(Array.from(new Set(post.tags.map(normalizeTag).filter((tag) => tag !== ''))).slice(0, 7));
    setEditInitialContentJson(post.contentJson);
    setEditSnapshot(createCommunityEditorSnapshotFromJson(post.contentJson));
    setIsEditing(true);
    setFeedback(null);
  }

  function closeEditMode() {
    setIsEditing(false);
    setIsEditCategoryMenuOpen(false);
    setEditDraftTag('');
    setIsSavingEdit(false);
  }

  function handleAddEditTag(rawValue = editDraftTag) {
    const normalizedTag = normalizeTag(rawValue);

    if (normalizedTag === '') {
      setEditDraftTag('');
      return;
    }

    setEditTags((currentTags) => {
      if (currentTags.includes(normalizedTag)) {
        return currentTags;
      }

      if (currentTags.length >= 7) {
        setFeedback('태그는 최대 7개까지 추가할 수 있다.');
        return currentTags;
      }

      return [...currentTags, normalizedTag];
    });
    setEditDraftTag('');
  }

  function handleRemoveEditTag(tagToRemove: string) {
    setEditTags((currentTags) => currentTags.filter((tag) => tag !== tagToRemove));
  }

  function selectEditCategory(nextCategory: EditableCommunityCategory) {
    setEditCategory(nextCategory);
    setIsEditCategoryMenuOpen(false);
    setFeedback(null);
  }

  function renderEditCategorySelect() {
    return (
      <div className="community-category-select-wrap community-title-category-wrap">
        <button
          type="button"
          className="community-title-category-trigger"
          onClick={() => setIsEditCategoryMenuOpen((currentValue) => !currentValue)}
          aria-haspopup="menu"
          aria-expanded={isEditCategoryMenuOpen}
          aria-label="게시글 구분 선택"
        >
          <span>{getCategoryLabel(editCategory)}</span>
          <CategoryArrowIcon />
        </button>

        {isEditCategoryMenuOpen ? (
          <div className="community-category-select-menu community-title-category-menu" role="menu">
            {editCategoryOptions.map((option) => (
              <button
                key={option}
                type="button"
                className={`community-category-select-item ${option === editCategory ? 'is-selected' : ''}`.trim()}
                onClick={() => selectEditCategory(option)}
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

  function renderEditActions() {
    return (
      <div className="community-detail-tab-actions community-title-edit-actions community-detail-icon-actions">
        <button type="button" className="community-detail-icon-button is-cancel" onClick={closeEditMode} aria-label="수정 취소">
          <CancelEditIcon />
        </button>
        <button
          type="button"
          className="community-detail-icon-button is-confirm"
          onClick={() => void handleSaveEdit()}
          disabled={isSavingEdit}
          aria-label={isSavingEdit ? '저장 중' : '저장'}
        >
          <SaveEditIcon />
        </button>
      </div>
    );
  }

  async function handleSaveEdit() {
    const normalizedTitle = editTitle.trim();

    if (normalizedTitle === '') {
      setFeedback('제목을 입력해야 한다.');
      return;
    }

    if (editSnapshot.empty) {
      setFeedback('본문을 입력해야 한다.');
      return;
    }

    if (editSnapshot.contentByteLength > COMMUNITY_POST_CONTENT_MAX_BYTES) {
      setFeedback('본문은 최대 500000 Byte까지 입력할 수 있다.');
      return;
    }

    setIsSavingEdit(true);

    try {
      await updateCommunityPost(postId, {
        title: normalizedTitle,
        category: editCategory,
        tags: Array.from(new Set(editTags.map(normalizeTag).filter((tag) => tag !== ''))).slice(0, 7),
        contentJson: editSnapshot.contentJson,
        plainTextSummary: editSnapshot.plainTextSummary,
        imageIds: editSnapshot.imageIds,
      });
      setIsEditing(false);
      setFeedback(null);
      const didReload = await reloadPost();
      if (didReload) {
        showSessionToast('게시글 수정 완료.');
      }
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '게시글 수정에 실패했다.');
    } finally {
      setIsSavingEdit(false);
    }
  }

  async function handleToggleLike() {
    if (!isAuthenticated) {
      setFeedback(null);
      openLoginOverlay();
      return;
    }

    try {
      const reaction = await toggleCommunityPostLike(postId);
      setPost((currentPost) =>
        currentPost
          ? {
              ...currentPost,
              likes: reaction.likeCount,
              likedByCurrentUser: reaction.liked,
            }
          : currentPost,
      );
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '좋아요 처리에 실패했다.');
    }
  }

  function handleCommentDraftKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void handleSubmitComment();
    }
  }

  async function handleSubmitComment() {
    if (!isAuthenticated) {
      setFeedback(null);
      openLoginOverlay(commentDraft.trim() ? COMMENT_LOGIN_DESCRIPTION : undefined);
      return;
    }

    if (!commentDraft.trim()) {
      return;
    }

    try {
      await addCommunityComment(postId, {
        content: commentDraft,
      });
      setCommentDraft('');
      await reloadPost();
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '댓글 등록에 실패했다.');
    }
  }

  function handleToggleReply(commentId: string) {
    setActiveReplyId((currentId) => (currentId === commentId ? null : commentId));
  }

  function handleChangeReplyDraft(commentId: string, value: string) {
    setReplyDrafts((currentDrafts) => ({
      ...currentDrafts,
      [commentId]: value,
    }));
  }

  async function handleSubmitReply(commentId: string) {
    if (!isAuthenticated) {
      setFeedback(null);
      openLoginOverlay(replyDrafts[commentId]?.trim() ? COMMENT_LOGIN_DESCRIPTION : undefined);
      return;
    }

    const replyDraft = replyDrafts[commentId]?.trim();

    if (!replyDraft) {
      return;
    }

    try {
      await addCommunityComment(postId, {
        content: replyDraft,
        parentCommentId: Number(commentId),
      });
      setReplyDrafts((currentDrafts) => ({
        ...currentDrafts,
        [commentId]: '',
      }));
      setActiveReplyId(null);
      await reloadPost();
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '대댓글 등록에 실패했다.');
    }
  }

  async function handleToggleCommentLike(commentId: string) {
    if (!isAuthenticated) {
      setFeedback(null);
      openLoginOverlay();
      return;
    }

    try {
      await toggleCommunityCommentLike(commentId);
      await reloadPost();
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '댓글 좋아요 처리에 실패했다.');
    }
  }

  async function handleCopyLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setFeedback(null);
      showSessionToast('링크 복사 완료.');
    } catch {
      setFeedback('링크 복사에 실패했다.');
    }
  }

  async function handleDeletePost() {
    if (!window.confirm('게시글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteCommunityPost(postId);
      handleBack();
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '게시글 삭제에 실패했다.');
    }
  }

  function handleBack() {
    navigate(COMMUNITY_PATH);
  }

  function handleContentClick(event: MouseEvent<HTMLDivElement>) {
    if (isEditing) {
      return;
    }

    const target = event.target;

    if (!(target instanceof HTMLImageElement)) {
      return;
    }

    setLightboxImage({
      src: target.currentSrc || target.src,
      alt: target.alt || post?.title || '',
    });
  }

  function openTagSearch(tag: string) {
    window.open(`${window.location.origin}${COMMUNITY_PATH}?tag=${encodeURIComponent(tag)}`, '_blank', 'noopener,noreferrer');
    setHoveredTag(null);
  }

  function openProblem(tag: string) {
    window.open(`${window.location.origin}${PROBLEMS_PATH}/${encodeURIComponent(tag)}`, '_blank', 'noopener,noreferrer');
    setHoveredTag(null);
  }

  const visibleTags = useMemo(() => Array.from(new Set(post?.tags ?? [])), [post?.tags]);

  if (isLoading) {
    return (
      <div className="page-stack community-detail-page">
        <section className="panel-card community-detail-card community-detail-loading-card">
          <div className="community-detail-topbar">
            <div className="solve-dbms-tab-row community-detail-tab-row" aria-hidden="true">
              <span className="solve-dbms-tab is-selected community-detail-category-tab community-detail-loading-tab">
                <span className="community-loading-placeholder is-short" />
              </span>
              <FavoriteTabButton className="favorite-tab-toggle-end" label={`커뮤니티 / ${postId}`} path={`${COMMUNITY_PATH}/${encodeURIComponent(postId)}`} />
            </div>
          </div>

          <div className="community-detail-loading-shell is-loading">
            <div className="community-detail-header community-detail-loading-body" aria-hidden="true">
              <span className="community-loading-placeholder is-long" />
              <div className="community-detail-tags">
                <span className="community-loading-placeholder is-short" />
                <span className="community-loading-placeholder is-short" />
              </div>
              <div className="community-detail-meta">
                <span className="community-loading-placeholder is-medium" />
                <span className="community-loading-placeholder is-medium" />
                <span className="community-loading-placeholder is-short" />
              </div>
              <div className="community-content-body">
                <span className="community-loading-placeholder is-long" />
                <span className="community-loading-placeholder is-long" />
                <span className="community-loading-placeholder is-medium" />
              </div>
            </div>

            <div className="community-table-loading-overlay" aria-live="polite" aria-label="로딩 중" />
          </div>
        </section>

        <section className="panel-card community-comments-card community-detail-loading-card">
          <div className="community-detail-loading-shell is-loading">
            <div className="community-detail-loading-comments" aria-hidden="true">
              <span className="community-loading-placeholder is-medium" />
              <span className="community-loading-placeholder is-long" />
              <span className="community-loading-placeholder is-long" />
            </div>

            <div className="community-table-loading-overlay" aria-live="polite" aria-label="로딩 중" />
          </div>
        </section>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="page-stack community-detail-page">
        <section id="community-post-detail" className="panel-card community-detail-card">
          <PageLoadFailureState />
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack community-detail-page">
      <section className="panel-card community-detail-card">
        {!isEditing ? (
          <div className="community-detail-topbar">
            <div className="solve-dbms-tab-row community-detail-tab-row" aria-label="게시글 구분">
              <span className="solve-dbms-tab is-selected community-detail-category-tab">{getCategoryLabel(post.category)}</span>

              <div className="community-detail-tab-actions community-detail-icon-actions">
                <button type="button" className="community-detail-icon-button" onClick={handleCopyLink} aria-label="링크 복사">
                  <LinkCopyIcon />
                </button>
                {post.editable ? (
                  <button type="button" className="community-detail-icon-button" onClick={openEditMode} aria-label="수정하기">
                    <EditIcon />
                  </button>
                ) : null}
                {post.editable ? (
                  <button type="button" className="community-detail-icon-button is-danger" onClick={() => void handleDeletePost()} aria-label="삭제하기">
                    <DeleteIcon />
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        ) : null}

        <div className="community-detail-header">
          {isEditing ? (
            <div className="community-title-input-row community-title-edit-row">
              {renderEditCategorySelect()}

              <input
                type="text"
                value={editTitle}
                onChange={(event) => setEditTitle(event.target.value)}
                onFocus={() => setIsEditCategoryMenuOpen(false)}
                className="text-field community-detail-title-input"
                placeholder="제목"
              />

              {renderEditActions()}
            </div>
          ) : (
            <h1 className="community-detail-title">{post.title}</h1>
          )}

          {isEditing ? (
            <div className="community-detail-edit-tags">
              {editTags.length > 0 ? (
                <div className="community-detail-edit-tag-list">
                  {editTags.map((tag) => (
                    <button key={tag} type="button" className="community-detail-edit-tag" onClick={() => handleRemoveEditTag(tag)}>
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
                value={editDraftTag}
                onChange={(event) => setEditDraftTag(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ',') {
                    event.preventDefault();
                    handleAddEditTag();
                  }
                }}
                className="text-field community-detail-edit-tag-input"
                placeholder={editTags.length >= 7 ? '태그는 최대 7개' : '태그 추가'}
              />
            </div>
          ) : visibleTags.length > 0 ? (
            <div className="community-detail-tags">
              {visibleTags.map((tag) => (
                <div
                  key={tag}
                  className="community-detail-tag-wrap"
                  onMouseEnter={() => setHoveredTag(tag)}
                  onMouseLeave={() => setHoveredTag((currentTag) => (currentTag === tag ? null : currentTag))}
                >
                  <button type="button" className="community-detail-tag">
                    #{tag}
                  </button>

                  {hoveredTag === tag ? (
                    <div className="community-tag-hover-menu">
                      <button type="button" className="community-tag-hover-item" onClick={() => openTagSearch(tag)}>
                        커뮤니티에서 태그 검색
                      </button>
                      {isProblemTag(tag) ? (
                        <button type="button" className="community-tag-hover-item" onClick={() => openProblem(tag)}>
                          문제로 이동
                        </button>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          ) : null}

          <div className="community-detail-meta">
            <button type="button" className="community-author-button" onClick={() => navigate(getProfilePath(post.authorHandle))}>
              <span>{post.authorHandle}</span>
            </button>
            <span>{formatBoardDate(post.createdAt)}</span>
            <span className="community-detail-metric" aria-label={`조회수 ${numberFormatter.format(post.views)}`}>
              <ViewIcon />
              <span>{numberFormatter.format(post.views)}</span>
            </span>
            <button
              type="button"
              className={`community-meta-like-button ${post.likedByCurrentUser ? 'is-liked' : ''}`.trim()}
              onClick={handleToggleLike}
              aria-pressed={post.likedByCurrentUser}
              aria-label={post.likedByCurrentUser ? '좋아요 취소' : '좋아요'}
            >
              <LikeIcon />
              <span>{numberFormatter.format(post.likes)}</span>
            </button>
            <span className="community-detail-metric" aria-label={`댓글 ${numberFormatter.format(post.comments)}`}>
              <CommentIcon />
              <span>{numberFormatter.format(post.comments)}</span>
            </span>
            {post.updatedAt ? <span>수정 {formatBoardDate(post.updatedAt)}</span> : null}
          </div>

          <div className="community-content-body" onClick={handleContentClick}>
            {isEditing ? (
              <CommunityTiptapEditor
                initialContentJson={editInitialContentJson}
                placeholder=""
                onSnapshot={(snapshot) => {
                  setEditSnapshot(snapshot);
                  setFeedback(null);
                }}
                onUploadImage={uploadCommunityImage}
                onFeedback={setFeedback}
              />
            ) : (
              <CommunityTiptapViewer contentJson={post.contentJson} />
            )}
          </div>
        </div>
      </section>

      {feedback ? (
        <section className="panel-card compact community-feedback-card">
          <p className="community-feedback-text">{feedback}</p>
        </section>
      ) : null}

      <section className="panel-card community-comments-card">
        <div className="community-comments-heading">
          <h2 className="panel-title">댓글 {numberFormatter.format(post.comments)}개</h2>
        </div>

        <div className="community-comment-compose">
          <div className="community-comment-compose-field">
            <textarea
              id="community-comment-draft"
              className="text-field community-comment-textarea community-comment-textarea-main"
              value={commentDraft}
              onChange={(event) => setCommentDraft(event.target.value)}
              onKeyDown={handleCommentDraftKeyDown}
              placeholder="댓글 추가"
            />
            <button type="button" className="community-comment-submit-icon" onClick={() => void handleSubmitComment()} aria-label="댓글 등록">
              <SendIcon />
            </button>
          </div>
        </div>

        <div className="community-comment-list">
          {post.commentTree.length > 0 ? (
            post.commentTree.map((comment) => (
              <CommunityCommentThread
                key={comment.id}
                comment={comment}
                activeReplyId={activeReplyId}
                replyDrafts={replyDrafts}
                onToggleReply={handleToggleReply}
                onChangeReplyDraft={handleChangeReplyDraft}
                onSubmitReply={handleSubmitReply}
                onToggleLike={handleToggleCommentLike}
              />
            ))
          ) : null}
        </div>
      </section>

      {lightboxImage ? (
        <div className="community-lightbox" role="dialog" aria-modal="true" aria-label="첨부 이미지 크게 보기">
          <button type="button" className="community-lightbox-backdrop" onClick={() => setLightboxImage(null)} />
          <div className="community-lightbox-panel">
            <button type="button" className="btn ghost community-lightbox-close" onClick={() => setLightboxImage(null)}>
              닫기
            </button>
            <img src={lightboxImage.src} alt={lightboxImage.alt} className="community-lightbox-image" />
          </div>
        </div>
      ) : null}
    </div>
  );
}
