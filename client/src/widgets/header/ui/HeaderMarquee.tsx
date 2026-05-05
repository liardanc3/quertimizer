import { useEffect, useRef, useState, type CSSProperties } from 'react';
import {
  DEFAULT_NOTIFICATION_TEXT,
  NOTIFICATION_UI_TEXT_KEY,
  useUiText,
  useUiTextValue,
} from '@/shared/config/ui-text';

export default function HeaderMarquee() {
  const { text } = useUiText();
  const marqueeMessage = useUiTextValue(NOTIFICATION_UI_TEXT_KEY, DEFAULT_NOTIFICATION_TEXT);
  const marqueeShellRef = useRef<HTMLDivElement | null>(null);
  const marqueeCopyRef = useRef<HTMLSpanElement | null>(null);
  const [marqueeMetrics, setMarqueeMetrics] = useState<{
    startOffset: number;
    endOffset: number;
    durationSeconds: number;
  } | null>(null);

  useEffect(() => {
    const marqueeShell = marqueeShellRef.current;
    const marqueeCopy = marqueeCopyRef.current;
    if (!marqueeShell || !marqueeCopy) {
      return;
    }

    let animationFrameId = 0;

    function updateMarqueeMetrics() {
      animationFrameId = window.requestAnimationFrame(() => {
        const currentMarqueeShell = marqueeShellRef.current;
        const currentMarqueeCopy = marqueeCopyRef.current;
        if (!currentMarqueeShell || !currentMarqueeCopy) {
          return;
        }

        const shellWidth = Math.ceil(currentMarqueeShell.getBoundingClientRect().width);
        const copyWidth = Math.ceil(currentMarqueeCopy.scrollWidth);
        if (shellWidth <= 0 || copyWidth <= 0) {
          return;
        }

        const durationSeconds = Math.max(Number(((shellWidth + copyWidth) / 92).toFixed(2)), 12);

        setMarqueeMetrics((currentMetrics) =>
          currentMetrics != null &&
          currentMetrics.startOffset === shellWidth &&
          currentMetrics.endOffset === -copyWidth &&
          currentMetrics.durationSeconds === durationSeconds
            ? currentMetrics
            : {
                startOffset: shellWidth,
                endOffset: -copyWidth,
                durationSeconds,
              },
        );
      });
    }

    updateMarqueeMetrics();

    const resizeObserver =
      typeof ResizeObserver === 'undefined'
        ? null
        : new ResizeObserver(() => {
            window.cancelAnimationFrame(animationFrameId);
            updateMarqueeMetrics();
          });

    resizeObserver?.observe(marqueeShell);
    resizeObserver?.observe(marqueeCopy);

    return () => {
      window.cancelAnimationFrame(animationFrameId);
      resizeObserver?.disconnect();
    };
  }, [marqueeMessage]);

  const marqueeTrackStyle =
    marqueeMetrics == null
      ? undefined
      : ({
          '--header-marquee-start': `${marqueeMetrics.startOffset}px`,
          '--header-marquee-end': `${marqueeMetrics.endOffset}px`,
          '--header-marquee-duration': `${marqueeMetrics.durationSeconds}s`,
        } as CSSProperties);

  return (
    <div ref={marqueeShellRef} className="header-marquee-shell" aria-label={text('HEADER_NOTICE_LABEL', '긴급 공지')}>
      <div className="header-marquee-track" style={marqueeTrackStyle}>
        <span ref={marqueeCopyRef} className="header-marquee-copy">
          {marqueeMessage}
        </span>
      </div>
    </div>
  );
}
