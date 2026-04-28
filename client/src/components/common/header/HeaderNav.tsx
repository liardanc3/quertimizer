import { navigate } from '../../../lib/navigation';

export interface HeaderNavItem {
  key: string;
  label: string;
  path: string;
  isActive: boolean;
}

export default function HeaderNav({ items, label }: { items: HeaderNavItem[]; label: string }) {
  return (
    <nav className="header-nav" aria-label={label}>
      {items.map((item) => (
        <button
          type="button"
          key={item.key}
          className={`nav-pill ${item.isActive ? 'is-active' : ''}`}
          onClick={() => navigate(item.path)}
        >
          {item.label}
        </button>
      ))}
    </nav>
  );
}
