import { useEffect, type RefObject } from 'react';

interface UseDismissableLayerOptions {
  enabled: boolean;
  refs: Array<RefObject<HTMLElement | null>>;
  onDismiss: () => void;
  dismissOnEscape?: boolean;
  dismissOnOutsidePointerDown?: boolean;
  dismissOnResize?: boolean;
  dismissOnScroll?: boolean;
  shouldIgnoreOutsidePointerDown?: (target: HTMLElement | null) => boolean;
}

export default function useDismissableLayer({
  enabled,
  refs,
  onDismiss,
  dismissOnEscape = true,
  dismissOnOutsidePointerDown = true,
  dismissOnResize = false,
  dismissOnScroll = false,
  shouldIgnoreOutsidePointerDown,
}: UseDismissableLayerOptions) {
  useEffect(() => {
    if (!enabled) {
      return;
    }

    function containsTarget(target: Node) {
      return refs.some((ref) => ref.current?.contains(target));
    }

    function handlePointerDown(event: MouseEvent) {
      if (!dismissOnOutsidePointerDown) {
        return;
      }

      const targetElement = event.target instanceof HTMLElement ? event.target : null;
      if (shouldIgnoreOutsidePointerDown?.(targetElement)) {
        return;
      }

      if (event.target instanceof Node && !containsTarget(event.target)) {
        onDismiss();
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (dismissOnEscape && event.key === 'Escape') {
        onDismiss();
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);

    if (dismissOnResize) {
      window.addEventListener('resize', onDismiss);
    }

    if (dismissOnScroll) {
      window.addEventListener('scroll', onDismiss, true);
    }

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);

      if (dismissOnResize) {
        window.removeEventListener('resize', onDismiss);
      }

      if (dismissOnScroll) {
        window.removeEventListener('scroll', onDismiss, true);
      }
    };
  }, [
    dismissOnEscape,
    dismissOnOutsidePointerDown,
    dismissOnResize,
    dismissOnScroll,
    enabled,
    onDismiss,
    refs,
    shouldIgnoreOutsidePointerDown,
  ]);
}
