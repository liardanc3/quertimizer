import type { CommunityPostSummary } from '../../types/domain';
import CommunityPostCard from './CommunityPostCard';

interface CommunityPostListProps {
  posts: CommunityPostSummary[];
}

export default function CommunityPostList({ posts }: CommunityPostListProps) {
  if (posts.length === 0) {
    return (
      <section className="community-board-table is-empty">
        <div className="problem-empty-state community-empty-state">
          선택한 조건에 맞는 게시글이 없습니다. 다른 검색어로 다시 찾아보세요.
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
      </div>

      {posts.map((post) => (
        <CommunityPostCard key={post.id} post={post} />
      ))}
    </section>
  );
}
