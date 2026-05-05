import type { MouseEvent } from 'react';
import { navigate } from '@/shared/config/navigation';

export interface HeaderNavItem {
  key: string;
  label: string;
  path: string;
  isActive: boolean;
}

function handleNavClick(event: MouseEvent<HTMLAnchorElement>, path: string) {
  if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.altKey || event.ctrlKey || event.shiftKey) {
    return;
  }

  event.preventDefault();
  navigate(path);
}

export default function HeaderNav({ items, label }: { items: HeaderNavItem[]; label: string }) {
  return (
    <nav className="header-nav" aria-label={label}>
      {items.map((item) => (
        <a
          href={item.path}
          key={item.key}
          className={`nav-pill ${item.isActive ? 'is-active' : ''}`}
          aria-current={item.isActive ? 'page' : undefined}
          onClick={(event) => handleNavClick(event, item.path)}
        >
          {item.label}
        </a>
      ))}
    </nav>
  );
}
