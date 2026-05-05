import { useEffect, useMemo, useState } from 'react';
import { FavoriteTabButton } from '@/features/favorite-tab';
import { GUIDE_PATH } from '@/shared/config/navigation';
import { getResolvedUiTextEntries, useUiText } from '@/shared/config/ui-text';
import './GuidePage.css';

interface GuideItem {
  id: number;
  title: string;
  body: string;
}

function findGuideValue(entryMap: Map<string, string>, upperKey: string, lowerKey: string) {
  return entryMap.get(upperKey) ?? entryMap.get(lowerKey) ?? '';
}

function buildGuideItems() {
  const entryMap = new Map(getResolvedUiTextEntries().map((entry) => [entry.key, entry.value]));
  const guideItems: GuideItem[] = [];

  for (let index = 1; index <= 50; index += 1) {
    const title = findGuideValue(entryMap, `GUIDE_TITLE_${index}`, `guide_title_${index}`).trim();
    const body = findGuideValue(entryMap, `GUIDE_BODY_${index}`, `guide_body_${index}`).trim();

    if (title === '' && body === '') {
      continue;
    }

    if (title === '' || body === '') {
      continue;
    }

    guideItems.push({
      id: index,
      title,
      body,
    });
  }

  return guideItems;
}

function renderGuideBody(body: string) {
  return body.split(/(quertimizer@gmail\.com)/gi).map((part, index) =>
    part.toLowerCase() === 'quertimizer@gmail.com'
      ? <a key={`guide-mail-${index}`} href={`mailto:${part}`}>{part}</a>
      : <span key={`guide-copy-${index}`}>{part}</span>,
  );
}

export default function GuidePage() {
  const { isReady, language, text } = useUiText();
  const guideItems = useMemo(buildGuideItems, [isReady, language]);
  const [activeGuideId, setActiveGuideId] = useState<number | null>(guideItems[0]?.id ?? null);
  const activeGuide = guideItems.find((guideItem) => guideItem.id === activeGuideId) ?? guideItems[0] ?? null;

  useEffect(() => {
    setActiveGuideId((currentGuideId) => {
      if (guideItems.length === 0) {
        return null;
      }

      if (currentGuideId != null && guideItems.some((guideItem) => guideItem.id === currentGuideId)) {
        return currentGuideId;
      }

      return guideItems[0].id;
    });
  }, [guideItems]);

  return (
    <div className="page-stack guide-page">
      {guideItems.length > 0 ? (
        <div className="guide-page-header">
          <div className="guide-page-tab-row solve-dbms-tab-row" role="tablist" aria-label={text('GUIDE_TABLIST_LABEL', '가이드')}>
            {guideItems.map((guideItem) => {
              const isSelected = guideItem.id === activeGuide?.id;

              return (
                <button
                  key={guideItem.id}
                  type="button"
                  role="tab"
                  aria-selected={isSelected}
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => setActiveGuideId(guideItem.id)}
                >
                  {guideItem.title}
                </button>
              );
            })}
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={text('GUIDE_FAVORITE_LABEL', { title: activeGuide?.title ?? text('HEADER_MENU_GUIDE', '가이드') }, `가이드 / ${activeGuide?.title ?? text('HEADER_MENU_GUIDE', '가이드')}`)}
              path={GUIDE_PATH}
            />
          </div>
        </div>
      ) : null}

      <section className="panel-card compact guide-page-card">
        <div className="guide-page-copy">
          {activeGuide != null ? <p className="guide-page-text">{renderGuideBody(activeGuide.body)}</p> : <p className="guide-page-text">{text('GUIDE_EMPTY_STATE', '내용이 없습니다.')}</p>}
        </div>
      </section>
    </div>
  );
}
