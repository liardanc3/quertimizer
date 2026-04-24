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
} from '../lib/communityApi';
import { getCommunityPostPath, getProfilePath, navigate } from '../lib/navigation';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { useMockSession } from '../lib/session';

interface ProfileActivityPageProps {
  handle?: string;
}

type ActivityTab = 'posts' | 'comments' | 'likes';

const numberFormatter = new Intl.NumberFormat('ko-KR');
const communityActivityLoadingRows = Array.from({ length: 5 }, (_, index) => index);

function formatBoardDate(value: string) {
  const date = new Date(value);
  const year = String(date.getFullYear()).slice(-2);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function readActiveTab(): ActivityTab {
  const tab = new URLSearchParams(window.location.search).get('tab');

  return tab === 'comments' || tab === 'likes' ? tab : 'posts';
}

export default function ProfileActivityPage({ handle: profileHandle }: ProfileActivityPageProps) {
  const { handle: currentHandle } = useMockSession();
  const resolvedHandle = profileHandle ?? currentHandle;
  const [activeTab, setActiveTab] = useState<ActivityTab>(readActiveTab());
  const [posts, setPosts] = useState<ProfileCommunityPost[]>([]);
  const [likedPosts, setLikedPosts] = useState<ProfileCommunityPost[]>([]);
  const [comments, setComments] = useState<ProfileCommunityComment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!resolvedHandle) {
      setPosts([]);
      setLikedPosts([]);
      setComments([]);
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setErrorMessage(null);

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

        setErrorMessage(error instanceof Error ? error.message : '활동 기록 조회에 실패했다.');
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
    { id: 'posts', label: '작성한 글', count: posts.length },
    { id: 'comments', label: '작성한 댓글', count: comments.length },
    { id: 'likes', label: '좋아요한 글', count: likedPosts.length },
  ], [comments.length, likedPosts.length, posts.length]);

  if (!resolvedHandle) {
    return null;
  }

  return (
    <div className="page-stack">
      <section className="panel-card community-activity-hero">
        <div className="community-detail-topbar">
          <button type="button" className="btn ghost community-back-button" onClick={() => navigate(getProfilePath(profileHandle))}>
            뒤로가기
          </button>
          <span className="subtle-chip">활동</span>
        </div>

        <div className="community-activity-header">
          <p className="panel-meta">활동 기록</p>
          <h1 className="page-title">커뮤니티 활동</h1>
          <p className="muted-text">작성한 글, 댓글, 좋아요한 글을 한곳에서 본다.</p>
        </div>

        <div className="community-activity-summary">
          {tabs.map((tab) => (
            <article key={tab.id} className="community-activity-summary-card">
              <p className="stat-label">{tab.label}</p>
              <strong className="community-activity-summary-value">{numberFormatter.format(tab.count)}</strong>
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
              <span className="tab-meta">{numberFormatter.format(tab.count)}</span>
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

            <div className="submit-history-loading-overlay" aria-hidden="true" />
          </div>
        ) : null}
        {!isLoading && errorMessage ? <PageLoadFailureState className="community-activity-empty" /> : null}

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
                    <span>{formatBoardDate(post.updatedAt ?? post.createdAt)}</span>
                  </div>
                  <p>{post.excerpt}</p>
                  <div className="community-activity-item-meta">
                    <span>좋아요 {numberFormatter.format(post.likeCount)}</span>
                    <span>댓글 {numberFormatter.format(post.commentCount)}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">아직 작성한 글이 없다.</div>
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
                    <span>{formatBoardDate(comment.createdAt)}</span>
                  </div>
                  <p>{comment.content}</p>
                  <div className="community-activity-item-meta">
                    <span>{comment.reply ? '대댓글' : '댓글'}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">아직 작성한 댓글이 없다.</div>
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
                    <span>{formatBoardDate(post.updatedAt ?? post.createdAt)}</span>
                  </div>
                  <p>{post.excerpt}</p>
                  <div className="community-activity-item-meta">
                    <span>좋아요 {numberFormatter.format(post.likeCount)}</span>
                    <span>댓글 {numberFormatter.format(post.commentCount)}</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="community-activity-empty">좋아요한 글이 아직 없다.</div>
          )
        ) : null}
      </section>
    </div>
  );
}
