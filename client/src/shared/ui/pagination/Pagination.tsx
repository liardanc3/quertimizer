import type { KeyboardEvent } from 'react';
import usePageJump from '@/shared/lib/hooks/use-page-jump';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  ariaLabel: string;
  inputLabel: string;
  inputOpenLabel: string;
  previousLabel: string;
  nextLabel: string;
  className?: string;
  pageButtonClassName?: string;
  metaClassName?: string;
  metaButtonClassName?: string;
  inputClassName?: string;
  hideWhenSinglePage?: boolean;
}

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  ariaLabel,
  inputLabel,
  inputOpenLabel,
  previousLabel,
  nextLabel,
  className = 'problem-pagination',
  pageButtonClassName = 'mini-toggle problem-page-button',
  metaClassName = 'problem-pagination-meta',
  metaButtonClassName = 'problem-pagination-meta-button',
  inputClassName = 'problem-pagination-meta-input',
  hideWhenSinglePage = false,
}: PaginationProps) {
  const pageJump = usePageJump({ currentPage, totalPages, onPageChange });

  if (hideWhenSinglePage && totalPages <= 1) {
    return null;
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      pageJump.applyPageJump();
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      pageJump.cancelPageJump();
    }
  }

  return (
    <div className={className} role="navigation" aria-label={ariaLabel}>
      <button
        type="button"
        className={pageButtonClassName}
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
      >
        {previousLabel}
      </button>

      {pageJump.isEditing ? (
        <input
          type="text"
          inputMode="numeric"
          className={inputClassName}
          aria-label={inputLabel}
          value={pageJump.draft}
          onChange={(event) => pageJump.setDraft(event.target.value)}
          onBlur={pageJump.applyPageJump}
          onKeyDown={handleKeyDown}
          autoFocus
        />
      ) : (
        <button
          type="button"
          className={`${metaClassName} ${metaButtonClassName}`.trim()}
          aria-label={inputOpenLabel}
          onClick={pageJump.openPageJump}
        >
          {`${currentPage} / ${totalPages}`}
        </button>
      )}

      <button
        type="button"
        className={pageButtonClassName}
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage >= totalPages}
      >
        {nextLabel}
      </button>
    </div>
  );
}
