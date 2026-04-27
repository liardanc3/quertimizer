import { useUiText } from '../../lib/uiText';
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
  const { text } = useUiText();

  if (posts.length === 0) {
    return (
      <section className="community-board-table is-empty">
        <div className="community-empty-state">
          <div className="community-empty-state-icon" aria-hidden="true">
            ⌁
          </div>
          <div className="community-empty-state-copy">
            <strong>{text('COMMUNITY_EMPTY_STATE', '조건에 맞는 게시글이 없습니다.')}</strong>
            <p>{text('COMMUNITY_EMPTY_GUIDE_MESSAGE', '검색어를 바꾸거나 필터를 해제해서 다시 찾아보세요.')}</p>
          </div>
          <button type="button" className="btn secondary" onClick={onResetFilters}>
            {text('COMMUNITY_SEARCH_RESET_BUTTON', '검색 초기화')}
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="community-board-table" role="table" aria-label={text('COMMUNITY_TABLE_LABEL', '커뮤니티 게시글 목록')}>
      <div className="community-board-head" role="row">
        <span role="columnheader">{text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}</span>
        <span role="columnheader">{text('COMMON_HANDLE_LABEL', 'Handle')}</span>
        <span role="columnheader">{text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}</span>
        <span role="columnheader">{text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}</span>
        <span role="columnheader">{text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}</span>
        <span role="columnheader">{text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}</span>
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
