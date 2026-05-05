import { useEffect, useState, type CSSProperties, type MouseEvent, type ReactNode } from 'react';

export interface FloatingPanelRect {
  left: number;
  top: number;
  width: number;
  height: number;
}

interface FloatingPanelWindowProps {
  meta: string;
  title: string;
  rect: FloatingPanelRect;
  zIndex: number;
  minWidth: number;
  minHeight: number;
  onRectChange: (nextRect: FloatingPanelRect) => void;
  onFocus: () => void;
  onAttach: () => void;
  onClose: () => void;
  children: ReactNode;
  style?: CSSProperties;
}

interface DragState {
  offsetX: number;
  offsetY: number;
}

interface ResizeState {
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

export default function FloatingPanelWindow({
  meta,
  title,
  rect,
  zIndex,
  minWidth,
  minHeight,
  onRectChange,
  onFocus,
  onAttach,
  onClose,
  children,
  style,
}: FloatingPanelWindowProps) {
  const [dragState, setDragState] = useState<DragState | null>(null);
  const [resizeState, setResizeState] = useState<ResizeState | null>(null);

  useEffect(() => {
    if (dragState == null && resizeState == null) {
      return;
    }

    const handleMouseMove = (event: MouseEvent | globalThis.MouseEvent) => {
      if (dragState != null) {
        const nextLeft = clamp(event.clientX - dragState.offsetX, 12, Math.max(12, window.innerWidth - rect.width - 12));
        const nextTop = clamp(event.clientY - dragState.offsetY, 12, Math.max(12, window.innerHeight - rect.height - 12));

        onRectChange({
          ...rect,
          left: nextLeft,
          top: nextTop,
        });
      }

      if (resizeState != null) {
        const maxWidth = Math.max(minWidth, window.innerWidth - rect.left - 12);
        const maxHeight = Math.max(minHeight, window.innerHeight - rect.top - 12);
        const nextWidth = clamp(resizeState.startWidth + (event.clientX - resizeState.startX), minWidth, maxWidth);
        const nextHeight = clamp(resizeState.startHeight + (event.clientY - resizeState.startY), minHeight, maxHeight);

        onRectChange({
          ...rect,
          width: nextWidth,
          height: nextHeight,
        });
      }
    };

    const handleMouseUp = () => {
      setDragState(null);
      setResizeState(null);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [dragState, minHeight, minWidth, onRectChange, rect, resizeState]);

  const handleHeaderMouseDown = (event: MouseEvent<HTMLDivElement>) => {
    if ((event.target as HTMLElement).closest('button')) {
      return;
    }

    event.preventDefault();
    onFocus();
    setDragState({
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
    });
  };

  const handleResizeMouseDown = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    onFocus();
    setResizeState({
      startX: event.clientX,
      startY: event.clientY,
      startWidth: rect.width,
      startHeight: rect.height,
    });
  };

  return (
    <section
      className="panel-card solve-pane solve-floating-window"
      style={{
        ...style,
        left: rect.left,
        top: rect.top,
        width: rect.width,
        height: rect.height,
        zIndex,
      }}
      onMouseDown={onFocus}
    >
      <div className="solve-floating-window-header" onMouseDown={handleHeaderMouseDown}>
        <div>
          <p className="panel-meta">{meta}</p>
          <h2 className="panel-title">{title}</h2>
        </div>

        <div className="solve-pane-actions">
          <button type="button" className="mini-toggle solve-pane-action solve-pane-action-icon is-selected" aria-label={`${title} 다시 붙이기`} onClick={onAttach}>
            <span aria-hidden="true">↙</span>
          </button>
          <button type="button" className="mini-toggle solve-pane-action solve-pane-action-icon" aria-label={`${title} 닫기`} onClick={onClose}>
            <span aria-hidden="true">×</span>
          </button>
        </div>
      </div>

      <div className="solve-floating-window-body">{children}</div>

      <button type="button" className="solve-floating-resizer" aria-label={`${title} 크기 조절`} onMouseDown={handleResizeMouseDown}>
        <span aria-hidden="true" />
      </button>
    </section>
  );
}
