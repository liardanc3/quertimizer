import { useEffect, useState } from 'react';

interface UsePageJumpOptions {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function usePageJump({ currentPage, totalPages, onPageChange }: UsePageJumpOptions) {
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(String(currentPage));

  useEffect(() => {
    if (isEditing) {
      return;
    }

    setDraft(String(currentPage));
  }, [currentPage, isEditing]);

  function normalizePage(value: string) {
    const parsedPage = Number.parseInt(value, 10);
    return Number.isNaN(parsedPage) ? currentPage : Math.min(totalPages, Math.max(1, parsedPage));
  }

  function applyPageJump() {
    const nextPage = normalizePage(draft);

    setDraft(String(nextPage));
    setIsEditing(false);

    if (nextPage !== currentPage) {
      onPageChange(nextPage);
    }
  }

  function cancelPageJump() {
    setDraft(String(currentPage));
    setIsEditing(false);
  }

  function openPageJump() {
    setDraft(String(currentPage));
    setIsEditing(true);
  }

  function updateDraft(value: string) {
    setDraft(value.replace(/\D+/g, ''));
  }

  return {
    isEditing,
    draft,
    setDraft: updateDraft,
    openPageJump,
    applyPageJump,
    cancelPageJump,
  };
}
