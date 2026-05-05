import type { ReactNode } from 'react';
import { SortIcon, type SortIconDirection } from '@/shared/ui/icons';

interface DataTableProps {
  className: string;
  label: string;
  isLoading?: boolean;
  children: ReactNode;
}

interface SortButtonProps {
  className: string;
  direction: SortIconDirection;
  label: string;
  onClick: () => void;
  active?: boolean;
  dataAttribute?: Record<string, string>;
}

export function DataTable({ className, label, isLoading = false, children }: DataTableProps) {
  return (
    <div className={`data-table ${className} ${isLoading ? 'is-loading' : ''}`.trim()} role="table" aria-label={label}>
      {children}
    </div>
  );
}

export function SortButton({ className, direction, label, onClick, active = false, dataAttribute }: SortButtonProps) {
  return (
    <button
      type="button"
      {...dataAttribute}
      className={`${className} ${active ? 'is-active' : ''}`.trim()}
      aria-label={label}
      onClick={onClick}
    >
      <SortIcon direction={direction} />
    </button>
  );
}
