import { useEffect, useRef, useState, type MouseEvent as ReactMouseEvent } from 'react';
import CommunityCommentThread from '../components/community/CommunityCommentThread';
import {
  addCommunityComment,
  deleteCommunityPost,
  fetchCommunityPostDetail,
  toggleCommunityCommentLike,
  toggleCommunityPostLike,
  type CommunityPostDetail,
} from '../lib/communityApi';
import { COMMUNITY_PATH, getCommunityPostEditPath, getProfilePath, PROBLEMS_PATH, navigate } from '../lib/navigation';
import { useMockSession } from '../lib/session';
import './CommunityPage.css';

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

function isProblemTag(tag: string) {
  return /^\d{5}-\d{5}$/.test(tag.trim());
}

export default function CommunityDetailPage({ postId }: CommunityDetailPageProps) {
  const { isAuthenticated } = useMockSession();
  const [post, setPost] = useState<CommunityPostDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [commentDraft, setCommentDraft] = useState('');
  const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({});
  const [activeReplyId, setActiveReplyId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [lightboxImage, setLightboxImage] = useState<{ src: string; alt: string } | null>(null);
  const [hoveredTag, setHoveredTag] = useState<string | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setErrorMessage(null);

    fetchCommunityPostDetail(postId)
      .then((nextPost) => {
        if (cancelled) {
          return;
        }

        setPost(nextPost);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setPost(null);
        setErrorMessage(error instanceof Error ? error.message : '게시글을 불러오지 못했다.');
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

  function handleBack() {
    if (window.history.state?.from) {
      window.history.back();
      return;
    }

    navigate(COMMUNITY_PATH);
  }

  async function reloadPost(feedbackMessage?: string) {
    try {
      const nextPost = await fetchCommunityPostDetail(postId);
      setPost(nextPost);
      if (feedbackMessage) {
        setFeedback(feedbackMessage);
      }
    } catch {
      setFeedback('게시글을 새로고침하지 못했다.');
    }
  }

  async function handleToggleLike() {
    if (!isAuthenticated) {
      setFeedback('로그인 후 이용할 수 있다.');
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
      setFeedback(reaction.liked ? '좋아요를 눌렀다.' : '좋아요를 취소했다.');
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '좋아요 처리에 실패했다.');
    }
  }

  async function handleSubmitComment() {
    if (!isAuthenticated) {
      setFeedback('로그인 후 이용할 수 있다.');
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
      await reloadPost('댓글을 등록했다.');
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
      setFeedback('로그인 후 이용할 수 있다.');
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
      await reloadPost('대댓글을 등록했다.');
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '대댓글 등록에 실패했다.');
    }
  }

  async function handleToggleCommentLike(commentId: string) {
    if (!isAuthenticated) {
      setFeedback('로그인 후 이용할 수 있다.');
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
    const path = `${window.location.origin}${window.location.pathname}`;

    try {
      await navigator.clipboard.writeText(path);
      setFeedback('게시글 링크를 복사했다.');
    } catch {
      setFeedback('링크 복사에 실패했다.');
    } finally {
      setIsMenuOpen(false);
    }
  }

  async function handleDeletePost() {
    try {
      await deleteCommunityPost(postId);
      setIsMenuOpen(false);
      handleBack();
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '게시글 삭제에 실패했다.');
    }
  }

  function handleContentClick(event: ReactMouseEvent<HTMLDivElement>) {
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

  if (isLoading) {
    return (
      <div className="page-stack community-detail-page">
        <section className="panel-card community-detail-card">
          <h1 className="community-detail-title">게시글을 불러오는 중이다.</h1>
        </section>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="page-stack community-detail-page">
        <section className="panel-card community-detail-card">
          <h1 className="community-detail-title">게시글을 찾을 수 없다.</h1>
          <p className="muted-text">{errorMessage ?? '삭제되었거나 잘못된 경로다.'}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack community-detail-page">
      <section className="panel-card community-detail-card">
        <div className="community-detail-topbar">
          <div className="community-detail-topbar-actions">
            <div className="community-detail-menu" ref={menuRef}>
              <button
                type="button"
                className={`mini-toggle community-detail-menu-button ${isMenuOpen ? 'is-selected' : ''}`}
                onClick={() => setIsMenuOpen((currentState) => !currentState)}
                aria-label="게시글 옵션"
                aria-haspopup="menu"
                aria-expanded={isMenuOpen}
              >
                ⋯
              </button>

              {isMenuOpen ? (
                <div className="community-detail-menu-panel" role="menu" aria-label="게시글 옵션">
                  {post.editable ? (
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
                  {post.editable ? (
                    <button
                      type="button"
                      className="community-detail-menu-item is-danger"
                      role="menuitem"
                      onClick={handleDeletePost}
                    >
                      삭제
                    </button>
                  ) : null}
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <div className="community-detail-header">
          <h1 className="community-detail-title">{post.title}</h1>

          <div className="community-detail-tags">
            {post.tags.map((tag) => (
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

          <div className="community-detail-meta">
            <button
              type="button"
              className="community-author-button"
              onClick={() => navigate(getProfilePath(post.authorHandle))}
            >
              <span>{post.authorHandle}</span>
            </button>
            <span>{formatBoardDate(post.createdAt)}</span>
            <span>조회수 {numberFormatter.format(post.views)}</span>
            <span>댓글 {numberFormatter.format(post.comments)}</span>
            {post.updatedAt ? <span>수정 {formatBoardDate(post.updatedAt)}</span> : null}
          </div>

          <div className="community-detail-actions">
            <button
              type="button"
              className={`community-like-button ${post.likedByCurrentUser ? 'is-liked' : ''}`}
              onClick={handleToggleLike}
              aria-pressed={post.likedByCurrentUser}
              aria-label={post.likedByCurrentUser ? '좋아요 취소' : '좋아요'}
            >
              <span className="community-like-icon" aria-hidden="true">
                👍
              </span>
              <span>{numberFormatter.format(post.likes)}</span>
            </button>
          </div>

          <div className="community-content-body" onClick={handleContentClick}>
            <p className="community-detail-lead">{post.excerpt}</p>
            <div
              className="community-detail-rich-content"
              dangerouslySetInnerHTML={{ __html: post.contentHtml || `<p>${post.content}</p>` }}
            />
          </div>
        </div>
      </section>

      {feedback ? (
        <section className="panel-card compact community-feedback-card">
          <p className="community-feedback-text">{feedback}</p>
        </section>
      ) : null}

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
            placeholder="댓글을 입력해라"
          />
          <div className="community-comment-compose-actions">
            <button type="button" className="btn primary" onClick={handleSubmitComment}>
              댓글 등록
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
          ) : (
            <div className="community-comment-empty">첫 댓글을 남겨라.</div>
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
