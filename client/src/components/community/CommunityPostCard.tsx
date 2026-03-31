import type { MouseEvent, ReactNode } from 'react';
import { getCommunityPostPath, getProfilePath, navigate } from '../../lib/navigation';
import type { CommunityPostSummary } from '../../types/domain';

interface CommunityPostCardProps {
  post: CommunityPostSummary;
  searchQuery: string;
  activeTag: string;
  onOpenPost: (postId: string) => void;
  onSelectTag: (tag: string) => void;
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
      </mark>
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
  function handleOpenPost(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    onOpenPost(post.id);
  }

  const highlightTerms = [searchQuery, activeTag].filter(Boolean);
  const shouldShowPreview = Boolean(searchQuery.trim() || activeTag.trim());
  const matchedTags = post.tags.filter((tag) => !activeTag || normalizeKeyword(tag) === normalizeKeyword(activeTag));

  return (
    <article className={`community-board-row ${post.isPinned ? 'is-pinned' : ''}`.trim()} role="row">
      <div className="community-board-cell community-board-title-cell" data-label="제목">
        <div className="community-row-title-line">
          {post.isPinned ? <span className="subtle-chip community-inline-chip">고정</span> : null}
          <a href={getCommunityPostPath(post.id)} className="community-post-title-button" onClick={handleOpenPost}>
            <strong className="community-row-title">{createHighlightParts(post.title, highlightTerms)}</strong>
          </a>
        </div>

        {shouldShowPreview ? (
          <div className="community-search-preview">
            <p className="community-search-preview-text">{createHighlightParts(post.excerpt, highlightTerms)}</p>
            {matchedTags.length > 0 ? (
              <div className="community-search-preview-tags">
                {matchedTags.map((tag) => (
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
          </div>
        ) : null}
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
        {formatBoardDate(post.updatedAt ?? post.createdAt)}
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
