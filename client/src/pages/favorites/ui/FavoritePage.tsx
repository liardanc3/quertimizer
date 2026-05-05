import { useSyncExternalStore } from 'react';
import { FavoriteStarIcon } from '@/features/favorite-tab';
import { getFavoriteTabsSnapshot, navigateToFavoriteTab, subscribeFavoriteTabs } from '@/features/favorite-tab';
import { useUiText } from '@/shared/config/ui-text';
import './FavoritePage.css';

export default function FavoritePage() {
  const { text } = useUiText();
  const favoriteTabs = useSyncExternalStore(subscribeFavoriteTabs, getFavoriteTabsSnapshot, () => []);

  return (
    <div className="page-stack favorite-page">
      <section className="panel-card compact problem-toolbar-card favorite-toolbar-card">
        <div className="problem-toolbar favorite-toolbar-stack">
          <div className="solve-dbms-tab-row favorite-tab-row" role="tablist" aria-label={text('FAVORITE_TABLIST_LABEL', '즐겨찾기 탭')}>
            {favoriteTabs.length > 0 ? (
              favoriteTabs.map((favoriteTab) => (
                <button
                  key={favoriteTab.path}
                  type="button"
                  role="tab"
                  aria-selected={false}
                  className="solve-dbms-tab favorite-nav-tab"
                  onClick={() => navigateToFavoriteTab(favoriteTab)}
                >
                  {favoriteTab.label}
                </button>
              ))
            ) : (
              <span className="solve-dbms-tab is-selected favorite-empty-tab" role="tab" aria-selected={true}>
                {text('FAVORITE_PAGE_TITLE', '즐겨찾기')}
              </span>
            )}
          </div>
        </div>
      </section>

      <section className="panel-card favorite-board">
        {favoriteTabs.length > 0 ? (
          <div className="favorite-empty-shell">
            <div className="favorite-empty-copy">
              <FavoriteStarIcon filled={true} className="favorite-empty-icon" />
              <p className="favorite-empty-title">{text('FAVORITE_SELECT_EMPTY_STATE', '즐겨찾기 탭을 선택해 이동하세요.')}</p>
            </div>
          </div>
        ) : (
          <div className="favorite-empty-shell is-empty">
            <div className="favorite-empty-copy">
              <FavoriteStarIcon filled={false} className="favorite-empty-icon" />
              <p className="favorite-empty-title">{text('FAVORITE_PAGE_EMPTY_STATE', '즐겨찾기 된 페이지가 없습니다.')}</p>
              <p className="favorite-empty-link">{text('FAVORITE_EMPTY_HELP', '상단 탭 오른쪽 별 아이콘으로 즐겨찾기를 추가하세요.')}</p>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
