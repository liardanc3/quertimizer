import type { CommunityPostSummary } from '../../types/domain';
import CommunityPostCard from './CommunityPostCard';

interface CommunityPostListProps {
  posts: CommunityPostSummary[];
  searchQuery: string;
  activeTag: string;
  onOpenPost: (postId: string) => void;
  onSelectTag: (tag: string) => void;
  onResetFilters: () => void;
}

export default function CommunityPostList({
  posts,
  searchQuery,
  activeTag,
  onOpenPost,
  onSelectTag,
  onResetFilters,
}: CommunityPostListProps) {
  if (posts.length === 0) {
    return (
      <section className="community-board-table is-empty">
        <div className="community-empty-state">
          <div className="community-empty-state-icon" aria-hidden="true">
            ⌁
          </div>
          <div className="community-empty-state-copy">
            <strong>조건에 맞는 게시글이 아직 없다.</strong>
            <p>검색어를 바꾸거나 필터를 해제해서 다시 찾아봐.</p>
          </div>
          <button type="button" className="btn secondary" onClick={onResetFilters}>
            검색 초기화
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="community-board-table" role="table" aria-label="커뮤니티 게시글 목록">
      <div className="community-board-head" role="row">
        <span role="columnheader">제목</span>
        <span role="columnheader">아이디</span>
        <span role="columnheader">작성일</span>
        <span role="columnheader">조회수</span>
        <span role="columnheader">좋아요</span>
        <span role="columnheader">댓글수</span>
      </div>

      {posts.map((post) => (
        <CommunityPostCard
          key={post.id}
          post={post}
          searchQuery={searchQuery}
          activeTag={activeTag}
          onOpenPost={onOpenPost}
          onSelectTag={onSelectTag}
        />
      ))}
    </section>
  );
}
