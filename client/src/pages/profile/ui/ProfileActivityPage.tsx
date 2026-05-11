import { useEffect, useMemo, useState } from 'react';
import {
  fetchCommunityCommentsByUser,
  fetchCommunityPostsByUser,
  fetchLikedPostsByUser,
  fetchMyCommunityComments,
  fetchMyCommunityPosts,
  fetchMyLikedPosts,
  type ProfileCommunityComment,
  type ProfileCommunityPost,
} from '@/shared/api/community-api';
import { HttpErrorState } from '@/shared/ui';
import { LoadingOverlay } from '@/shared/ui';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '@/shared/api/api-error';
import { getCommunityPostPath, getProfilePath, navigate } from '@/shared/config/navigation';
import { PageLoadFailureState } from '@/shared/ui';
import { useSession } from '@/shared/auth/session';
import { formatCompactBoardDate, formatInteger } from '@/shared/lib/formatters';
import { useUiText } from '@/shared/config/ui-text';

interface ProfileActivityPageProps {
  handle?: string;
}

type ActivityTab = 'posts' | 'comments' | 'likes';

const communityActivityLoadingRows = Array.from({ length: 5 }, (_, index) => index);

function readActiveTab(): ActivityTab {
  const tab = new URLSearchParams(window.location.search).get('tab');

  return tab === 'comments' || tab === 'likes' ? tab : 'posts';
}

export default function ProfileActivityPage({ handle: profileHandle }: ProfileActivityPageProps) {
  const { text } = useUiText();
  const { handle: currentHandle } = useSession();
  const resolvedHandle = profileHandle ?? currentHandle;
  const [activeTab, setActiveTab] = useState<ActivityTab>(readActiveTab());
  const [posts, setPosts] = useState<ProfileCommunityPost[]>([]);
  const [likedPosts, setLikedPosts] = useState<ProfileCommunityPost[]>([]);
  const [comments, setComments] = useState<ProfileCommunityComment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [errorStatus, setErrorStatus] = useState<number | null>(null);

  useEffect(() => {
    if (!resolvedHandle) {
      setPosts([]);
      setLikedPosts([]);
      setComments([]);
      setIsLoading(false);
      setErrorStatus(null);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setErrorMessage(null);
    setErrorStatus(null);

    const loadPosts = profileHandle ? fetchCommunityPostsByUser(resolvedHandle) : fetchMyCommunityPosts();
    const loadLikedPosts = profileHandle ? fetchLikedPostsByUser(resolvedHandle) : fetchMyLikedPosts();
    const loadComments = profileHandle ? fetchCommunityCommentsByUser(resolvedHandle) : fetchMyCommunityComments();

    Promise.all([loadPosts, loadLikedPosts, loadComments])
      .then(([nextPosts, nextLikedPosts, nextComments]) => {
        if (cancelled) {
          return;
        }

        setPosts(nextPosts);
        setLikedPosts(nextLikedPosts);
        setComments(nextComments);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        const status = getApiErrorStatus(error);
        setErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [profileHandle, resolvedHandle]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    params.set('tab', activeTab);
    const query = params.toString();
    const nextPath = `${window.location.pathname}${query ? `?${query}` : ''}`;

    window.history.replaceState(window.history.state ?? {}, '', nextPath);
  }, [activeTab]);

  const tabs: Array<{ id: ActivityTab; label: string; count: number }> = useMemo(() => [
    { id: 'posts', label: text('PROFILE_ACTIVITY_TAB_POSTS_LABEL', '작성한 글'), count: posts.length },
    { id: 'comments', label: text('PROFILE_ACTIVITY_TAB_COMMENTS_LABEL', '작성한 댓글'), count: comments.length },
    { id: 'likes', label: text('PROFILE_ACTIVITY_TAB_LIKES_LABEL', '좋아요한 글'), count: likedPosts.length },
  ], [comments.length, likedPosts.length, posts.length, text]);

  if (!resolvedHandle) {
    return null;
  }

  return (
    <div className="page-stack">
      <section className="panel-card community-activity-hero">
        <div className="community-detail-topbar">
          <button type="button" className="btn ghost community-back-button" onClick={() => navigate(getProfilePath(profileHandle))}>
            {text('PROFILE_ACTIVITY_BACK_BUTTON', '뒤로가기')}
          </button>
          <span className="subtle-chip">{text('PROFILE_ACTIVITY_BADGE', '활동')}</span>
        </div>

        <div className="community-activity-header">
          <p className="panel-meta">{text('PROFILE_ACTIVITY_PAGE_LABEL', '활동 기록')}</p>
          <h1 className="page-title">{text('PROFILE_ACTIVITY_TITLE', '커뮤니티 활동')}</h1>
          <p className="muted-text">{text('PROFILE_ACTIVITY_DESC', '작성한 글, 댓글, 좋아요한 글을 한곳에서 볼 수 있습니다.')}</p>
        </div>

        <div className="community-activity-summary">
          {tabs.map((tab) => (
            <article key={tab.id} className="community-activity-summary-card">
              <p className="stat-label">{tab.label}</p>
              <strong className="community-activity-summary-value">{formatInteger(tab.count)}</strong>
            </article>
          ))}
        </div>
      </section>

      <section className="panel-card community-activity-panel">
        <div className="community-activity-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`tab-button ${activeTab === tab.id ? 'is-selected' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
              <span className="tab-meta">{formatInteger(tab.count)}</span>
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="community-activity-loading-shell is-loading">
            <div className="community-activity-list" aria-hidden="true">
              {communityActivityLoadingRows.map((rowIndex) => (
                <div key={`community-activity-loading-${rowIndex}`} className="community-activity-item community-activity-loading-item">
                  <div className="community-activity-item-head">
                    <span className="wave-loading-placeholder is-long" />
                    <span className="wave-loading-placeholder is-medium" />
                  </div>
                  <p>
                    <span className="wave-loading-placeholder is-long" />
                  </p>
                  <div className="community-activity-item-meta community-activity-loading-meta">
                    <span className="wave-loading-placeholder is-short" />
                    <span className="wave-loading-placeholder is-short" />
                  </div>
                </div>
              ))}
            </div>

            <LoadingOverlay ariaHidden />
          </div>
        ) : null}
        {!isLoading && errorMessage
          ? errorStatus != null
            ? <HttpErrorState status={errorStatus} className="community-activity-empty" message={errorMessage} />
            : <PageLoadFailureState className="community-activity-empty" message={errorMessage} />
          : null}

        {!isLoading && !errorMessage && activeTab === 'posts' ? (
          posts.length > 0 ? (
            <div className="community-activity-list">
              {posts.map((post) => (
                <button
                  key={post.postId}
                  type="button"
                  className="community-activity-item"
                  onClick={() => navigate(getCommunityPostPath(post.postId))}
                >
                  <div className="community-activity-item-head">
                    <strong>{post.title}</strong>
                    <span>{formatCompactBoardDate(post.createdAt)}</span>
                  </div>
                  <p>{post.excerpt}</p>
                  <div className="community-activity-item-meta">
                    <span>{text('PROFILE_ACTIVITY_LIKES_COUNT_LABEL', { count: formatInteger(post.likeCount) }, `좋아요 ${formatInteger(post.likeCount)}`)}</span>
                    <span>{text('PROFILE_ACTIVITY_COMMENTS_COUNT_LABEL', { count: formatInteger(post.commentCount) }, `댓글 ${formatInteger(post.commentCount)}`)}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">{text('PROFILE_ACTIVITY_EMPTY_POSTS', '아직 작성한 글이 없습니다.')}</div>
          )
        ) : null}

        {!isLoading && !errorMessage && activeTab === 'comments' ? (
          comments.length > 0 ? (
            <div className="community-activity-list">
              {comments.map((comment) => (
                <button
                  key={comment.commentId}
                  type="button"
                  className="community-activity-item"
                  onClick={() => navigate(getCommunityPostPath(comment.postId))}
                >
                  <div className="community-activity-item-head">
                    <strong>{comment.postTitle}</strong>
                    <span>{formatCompactBoardDate(comment.createdAt)}</span>
                  </div>
                  <p>{comment.content}</p>
                  <div className="community-activity-item-meta">
                    <span>{comment.reply ? text('PROFILE_ACTIVITY_REPLY_LABEL', '대댓글') : text('PROFILE_ACTIVITY_COMMENT_LABEL', '댓글')}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">{text('PROFILE_ACTIVITY_EMPTY_COMMENTS', '아직 작성한 댓글이 없습니다.')}</div>
          )
        ) : null}

        {!isLoading && !errorMessage && activeTab === 'likes' ? (
          likedPosts.length > 0 ? (
            <div className="community-activity-list">
              {likedPosts.map((post) => (
                <button
                  key={post.postId}
                  type="button"
                  className="community-activity-item"
                  onClick={() => navigate(getCommunityPostPath(post.postId))}
                >
                  <div className="community-activity-item-head">
                    <strong>{post.title}</strong>
                    <span>{formatCompactBoardDate(post.createdAt)}</span>
                  </div>
                  <p>{post.excerpt}</p>
                  <div className="community-activity-item-meta">
                    <span>{text('PROFILE_ACTIVITY_LIKES_COUNT_LABEL', { count: formatInteger(post.likeCount) }, `좋아요 ${formatInteger(post.likeCount)}`)}</span>
                    <span>{text('PROFILE_ACTIVITY_COMMENTS_COUNT_LABEL', { count: formatInteger(post.commentCount) }, `댓글 ${formatInteger(post.commentCount)}`)}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">{text('PROFILE_ACTIVITY_EMPTY_LIKES', '좋아요한 글이 아직 없습니다.')}</div>
          )
        ) : null}
      </section>
    </div>
  );
}
