import { useEffect, useSyncExternalStore } from 'react';
import {
  getFavoriteTabsSnapshot,
  removeFavoriteTab,
  saveFavoriteTab,
  subscribeFavoriteTabs,
  type FavoriteTabEntry,
  type FavoriteTabSnapshot,
} from '../../lib/favoriteTabs';
import './FavoriteTabs.css';

export function FavoriteStarIcon({ filled = false, className = '' }: { filled?: boolean; className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" className={className}>
      <path
        d="m8 2.2 1.62 3.28 3.62.53-2.62 2.55.62 3.6L8 10.46l-3.24 1.7.62-3.6L2.76 6l3.62-.53L8 2.2Z"
        fill={filled ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth={filled ? '1.2' : '1.35'}
        strokeLinejoin="round"
      />
    </svg>
  );
}

interface FavoriteTabButtonProps {
  label: string;
  path: string;
  className?: string;
  snapshot?: FavoriteTabSnapshot | null;
  getSnapshot?: () => FavoriteTabSnapshot | null;
}

export default function FavoriteTabButton({ label, path, className = '', snapshot = null, getSnapshot }: FavoriteTabButtonProps) {
  const favoriteTabs = useSyncExternalStore(subscribeFavoriteTabs, getFavoriteTabsSnapshot, () => []);
  const isSelected = favoriteTabs.some((entry) => entry.path === path);

  function buildFavoriteEntry(): FavoriteTabEntry {
    return {
      label,
      path,
      snapshot: getSnapshot ? getSnapshot() : snapshot,
    };
  }

  useEffect(() => {
    if (!isSelected) {
      return;
    }

    saveFavoriteTab(buildFavoriteEntry());
  }, [getSnapshot, isSelected, label, path, snapshot]);

  return (
    <button
      type="button"
      className={`favorite-tab-toggle ${isSelected ? 'is-selected' : ''} ${className}`.trim()}
      aria-label={isSelected ? `${label} 즐겨찾기 해제` : `${label} 즐겨찾기 추가`}
      aria-pressed={isSelected}
      onClick={() => {
        if (isSelected) {
          removeFavoriteTab(path);
          return;
        }

        saveFavoriteTab(buildFavoriteEntry());
      }}
    >
      <FavoriteStarIcon filled={isSelected} className="favorite-tab-toggle-icon" />
    </button>
  );
}
