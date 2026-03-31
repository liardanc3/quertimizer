import { useEffect, useRef, useState, useSyncExternalStore, type MouseEvent as ReactMouseEvent } from 'react';
import CommunityCommentThread from '../components/community/CommunityCommentThread';
import {
  addCommunityComment,
  deleteCommunityPost,
  getCommunityComments,
  getCommunityPostById,
  getCommunityStoreSnapshot,
  isCommunityPostLiked,
  subscribeCommunityStore,
  toggleCommunityPostLike,
} from '../lib/communityStore';
import { COMMUNITY_PATH, getCommunityPostEditPath, navigate } from '../lib/navigation';
import { mockCurrentHandle } from '../mocks/profile';

interface CommunityDetailPageProps {
  postId: string;
}

const numberFormatter = new Intl.NumberFormat('ko-KR');

function formatBoardDate(value: string) {
  const date = new Date(value);
  const year = String(date.getFullYear()).slice(-2);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

export default function CommunityDetailPage({ postId }: CommunityDetailPageProps) {
  useSyncExternalStore(subscribeCommunityStore, getCommunityStoreSnapshot, () => '');

  const post = getCommunityPostById(postId);
  const comments = getCommunityComments(postId);
  const [commentDraft, setCommentDraft] = useState('');
  const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({});
  const [activeReplyId, setActiveReplyId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [lightboxImage, setLightboxImage] = useState<{ src: string; alt: string } | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const isLiked = isCommunityPostLiked(postId);

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    function handlePointerDown(event: globalThis.MouseEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsMenuOpen(false);
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isMenuOpen]);

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

  if (!post) {
    return (
      <div className="page-stack">
        <section className="panel-card community-detail-card">
          <button type="button" className="btn ghost community-back-button" onClick={() => navigate(COMMUNITY_PATH)}>
            뒤로가기
          </button>
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title">게시글을 찾을 수 없습니다.</h1>
          <p className="muted-text">삭제되었거나 잘못된 경로입니다. 목록으로 돌아가 다시 확인해 주세요.</p>
        </section>
      </div>
    );
  }

  const postTitle = post.title;

  function handleBack() {
    if (window.history.state?.from) {
      window.history.back();
      return;
    }

    navigate(COMMUNITY_PATH);
  }

  function handleToggleLike() {
    const nextLiked = toggleCommunityPostLike(postId);
    setFeedback(nextLiked ? '좋아요를 눌렀습니다.' : '좋아요를 취소했습니다.');
  }

  function handleSubmitComment() {
    if (!commentDraft.trim()) {
      return;
    }

    addCommunityComment({
      postId,
      content: commentDraft,
    });
    setCommentDraft('');
    setFeedback('댓글이 등록되었습니다.');
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

  function handleSubmitReply(commentId: string) {
    const replyDraft = replyDrafts[commentId]?.trim();

    if (!replyDraft) {
      return;
    }

    addCommunityComment({
      postId,
      content: replyDraft,
      parentId: commentId,
    });
    setReplyDrafts((currentDrafts) => ({
      ...currentDrafts,
      [commentId]: '',
    }));
    setActiveReplyId(null);
    setFeedback('대댓글이 등록되었습니다.');
  }

  function handleOpenTag(tag: string) {
    navigate(`${COMMUNITY_PATH}?tag=${encodeURIComponent(tag)}`);
  }

  async function handleCopyLink() {
    const path = `${window.location.origin}${window.location.pathname}`;

    try {
      await navigator.clipboard.writeText(path);
      setFeedback('게시글 링크를 복사했습니다.');
    } catch {
      setFeedback('링크 복사에 실패했습니다.');
    } finally {
      setIsMenuOpen(false);
    }
  }

  function handleDeletePost() {
    deleteCommunityPost(postId);
    setIsMenuOpen(false);
    handleBack();
  }

  function handleReportPost() {
    setIsMenuOpen(false);
    setFeedback('신고 기능은 운영 API 연결 전 단계입니다. 접수 UI만 먼저 반영했습니다.');
  }

  function handleContentClick(event: ReactMouseEvent<HTMLDivElement>) {
    const target = event.target;

    if (!(target instanceof HTMLImageElement)) {
      return;
    }

    setLightboxImage({
      src: target.currentSrc || target.src,
      alt: target.alt || postTitle,
    });
  }

  return (
    <div className="page-stack">
      <section className="panel-card community-detail-card">
        <div className="community-detail-topbar">
          <button type="button" className="btn ghost community-back-button" onClick={handleBack}>
            뒤로가기
          </button>

          <div className="community-detail-topbar-actions">
            {post.isPinned ? <span className="subtle-chip">고정글</span> : null}

            <div className="community-detail-menu" ref={menuRef}>
              <button
                type="button"
                className={`mini-toggle community-detail-menu-button ${isMenuOpen ? 'is-selected' : ''}`}
                onClick={() => setIsMenuOpen((currentState) => !currentState)}
                aria-label="게시글 옵션"
                aria-haspopup="menu"
                aria-expanded={isMenuOpen}
              >
                •••
              </button>

              {isMenuOpen ? (
                <div className="community-detail-menu-panel" role="menu" aria-label="게시글 옵션">
                  {post.authorHandle === mockCurrentHandle ? (
                    <button
                      type="button"
                      className="community-detail-menu-item"
                      role="menuitem"
                      onClick={() =>
                        navigate(getCommunityPostEditPath(postId), {
                          state: {
                            from: window.history.state?.from ?? COMMUNITY_PATH,
                          },
                        })
                      }
                    >
                      수정
                    </button>
                  ) : null}
                  <button type="button" className="community-detail-menu-item" role="menuitem" onClick={handleCopyLink}>
                    링크 복사
                  </button>
                  {post.authorHandle === mockCurrentHandle ? (
                    <button
                      type="button"
                      className="community-detail-menu-item is-danger"
                      role="menuitem"
                      onClick={handleDeletePost}
                    >
                      삭제
                    </button>
                  ) : (
                    <button type="button" className="community-detail-menu-item" role="menuitem" onClick={handleReportPost}>
                      신고
                    </button>
                  )}
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <div className="community-detail-header">
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title community-detail-title">{post.title}</h1>

          <div className="community-detail-meta">
            <span>@{post.authorHandle}</span>
            <span>{formatBoardDate(post.createdAt)}</span>
            <span>조회수 {numberFormatter.format(post.views)}</span>
            <span>좋아요 {numberFormatter.format(post.likes)}</span>
            {post.updatedAt ? <span>수정 {formatBoardDate(post.updatedAt)}</span> : null}
          </div>

          <div className="community-detail-tags">
            {post.tags.map((tag) => (
              <button key={tag} type="button" className="community-detail-tag" onClick={() => handleOpenTag(tag)}>
                #{tag}
              </button>
            ))}
          </div>

          <div className="community-detail-actions">
            <button
              type="button"
              className={`btn ${isLiked ? 'primary' : 'secondary'} community-like-button`}
              onClick={handleToggleLike}
            >
              {isLiked ? '좋아요 취소' : '좋아요'}
            </button>
            <span className="community-detail-reaction-count">댓글 {numberFormatter.format(post.comments)}개</span>
          </div>
        </div>
      </section>

      {feedback ? (
        <section className="panel-card compact community-feedback-card">
          <p className="community-feedback-text">{feedback}</p>
        </section>
      ) : null}

      <section className="panel-card community-content-card">
        <div className="community-content-body" onClick={handleContentClick}>
          <p className="community-detail-lead">{post.excerpt}</p>
          <div
            className="community-detail-rich-content"
            dangerouslySetInnerHTML={{ __html: post.contentHtml ?? `<p>${post.content}</p>` }}
          />
        </div>
      </section>

      <section className="panel-card community-comments-card">
        <div className="panel-heading-row responsive">
          <div>
            <p className="panel-meta">댓글</p>
            <h2 className="panel-title">댓글 {numberFormatter.format(post.comments)}개</h2>
          </div>
          <span className="subtle-chip">대댓글 지원</span>
        </div>

        <div className="community-comment-compose">
          <label className="field-label" htmlFor="community-comment-draft">
            댓글 작성
          </label>
          <textarea
            id="community-comment-draft"
            className="text-field community-comment-textarea"
            value={commentDraft}
            onChange={(event) => setCommentDraft(event.target.value)}
            placeholder="댓글을 입력하세요."
          />
          <div className="community-comment-compose-actions">
            <button type="button" className="btn primary" onClick={handleSubmitComment}>
              댓글 등록
            </button>
          </div>
        </div>

        <div className="community-comment-list">
          {comments.length > 0 ? (
            comments.map((comment) => (
              <CommunityCommentThread
                key={comment.id}
                comment={comment}
                activeReplyId={activeReplyId}
                replyDrafts={replyDrafts}
                onToggleReply={handleToggleReply}
                onChangeReplyDraft={handleChangeReplyDraft}
                onSubmitReply={handleSubmitReply}
              />
            ))
          ) : (
            <div className="community-comment-empty">첫 댓글을 남겨보세요.</div>
          )}
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
