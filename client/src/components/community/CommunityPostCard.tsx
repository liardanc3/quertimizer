import type { MouseEvent, ReactNode } from 'react';
import { getCommunityPostPath, getProfilePath, navigate } from '../../lib/navigation';
import { formatCompactBoardDate, formatInteger } from '../../lib/formatters';
import { useUiText } from '../../lib/uiText';
import type { CommunityPostSummary } from '../../types/domain';

interface CommunityPostCardProps {
  post: CommunityPostSummary;
  searchQuery: string;
  activeTag: string;
  onOpenPost: (postId: string) => void;
  onSelectTag: (tag: string) => void;
}

function normalizeKeyword(value: string) {
  return value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[_\-\s]+/g, '')
    .replace(/[^\p{L}\p{N}]/gu, '');
}

function createHighlightParts(text: string, rawTerms: string[]) {
  const terms = rawTerms.map((term) => term.trim()).filter(Boolean);

  if (terms.length === 0) {
    return [text];
  }

  const lowerText = text.toLowerCase();
  const matches: Array<{ start: number; end: number }> = [];

  for (const term of terms) {
    const normalizedTerm = normalizeKeyword(term);

    if (!normalizedTerm) {
      continue;
    }

    const lowerTerm = term.toLowerCase();
    let startIndex = 0;

    while (startIndex < text.length) {
      const directIndex = lowerText.indexOf(lowerTerm, startIndex);

      if (directIndex === -1) {
        break;
      }

      matches.push({ start: directIndex, end: directIndex + term.length });
      startIndex = directIndex + term.length;
    }
  }

  if (matches.length === 0) {
    return [text];
  }

  matches.sort((left, right) => left.start - right.start);
  const mergedMatches = matches.reduce<Array<{ start: number; end: number }>>((accumulator, match) => {
    const lastMatch = accumulator[accumulator.length - 1];

    if (!lastMatch || match.start > lastMatch.end) {
      accumulator.push({ ...match });
      return accumulator;
    }

    lastMatch.end = Math.max(lastMatch.end, match.end);
    return accumulator;
  }, []);

  const parts: ReactNode[] = [];
  let cursor = 0;

  mergedMatches.forEach((match, index) => {
    if (cursor < match.start) {
      parts.push(text.slice(cursor, match.start));
    }

    parts.push(
      <mark key={`${text}-${match.start}-${index}`} className="community-highlight">
        {text.slice(match.start, match.end)}
      </mark>,
    );
    cursor = match.end;
  });

  if (cursor < text.length) {
    parts.push(text.slice(cursor));
  }

  return parts;
}

export default function CommunityPostCard({
  post,
  searchQuery,
  activeTag,
  onOpenPost,
  onSelectTag,
}: CommunityPostCardProps) {
  const { text } = useUiText();

  function handleOpenPost(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    onOpenPost(post.id);
  }

  const highlightTerms = [searchQuery, activeTag].filter(Boolean);

  return (
    <article className="community-board-row" role="row">
      <div className="community-board-cell community-board-title-cell" data-label={text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}>
        {post.tags.length > 0 ? (
          <div className="community-row-tags">
            {post.tags.slice(0, 5).map((tag) => (
              <button
                key={tag}
                type="button"
                className="community-inline-tag-button"
                onClick={() => onSelectTag(tag)}
              >
                #{tag}
              </button>
            ))}
          </div>
        ) : null}

        <div className="community-row-title-line">
          <a href={getCommunityPostPath(post.id)} className="community-post-title-button" onClick={handleOpenPost}>
            <strong className="community-row-title">{createHighlightParts(post.title, highlightTerms)}</strong>
          </a>
        </div>

        {searchQuery.trim() !== '' ? (
          <p className="community-search-preview-text">{createHighlightParts(post.excerpt, highlightTerms)}</p>
        ) : null}
      </div>

      <div className="community-board-cell community-board-author" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>
        <button
          type="button"
          className="community-author-button"
          onClick={() => navigate(getProfilePath(post.authorHandle))}
        >
          <span className="community-author-id">{post.authorHandle}</span>
        </button>
      </div>

      <div className="community-board-cell community-board-date" data-label={text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}>
        {formatCompactBoardDate(post.updatedAt ?? post.createdAt)}
      </div>

      <div className="community-board-cell community-board-metric" data-label={text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}>
        {formatInteger(post.views)}
      </div>

      <div className="community-board-cell community-board-metric" data-label={text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}>
        {formatInteger(post.likes)}
      </div>

      <div className="community-board-cell community-board-metric" data-label={text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}>
        {formatInteger(post.comments)}
      </div>
    </article>
  );
}
