import { getProfilePath, navigate } from '../../lib/navigation';
import type { CommunityPostSummary } from '../../types/domain';

interface CommunityPostCardProps {
  post: CommunityPostSummary;
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

export default function CommunityPostCard({ post }: CommunityPostCardProps) {

  return (
    <article className={`community-board-row ${post.isPinned ? 'is-pinned' : ''}`.trim()} role="row">
      <div className="community-board-cell community-board-title-cell" data-label="제목">
        <div className="community-row-title-line">
          {post.isPinned ? <span className="subtle-chip community-inline-chip">고정</span> : null}
          <strong className="community-row-title">{post.title}</strong>
        </div>
      </div>

      <div className="community-board-cell community-board-author" data-label="아이디">
        <button
          type="button"
          className="btn text inline community-author-button"
          onClick={() => navigate(getProfilePath(post.authorHandle))}
        >
          <span className="community-author-id">@{post.authorHandle}</span>
        </button>
      </div>

      <div className="community-board-cell community-board-date" data-label="작성일">
        {formatBoardDate(post.createdAt)}
      </div>

      <div className="community-board-cell community-board-metric" data-label="조회수">
        {numberFormatter.format(post.views)}
      </div>

      <div className="community-board-cell community-board-metric" data-label="좋아요">
        {numberFormatter.format(post.likes)}
      </div>
    </article>
  );
}
