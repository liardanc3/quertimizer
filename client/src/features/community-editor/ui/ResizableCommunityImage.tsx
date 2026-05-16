import { NodeViewWrapper, type ReactNodeViewProps } from '@tiptap/react';
import { type PointerEvent, useMemo, useRef, useState } from 'react';
import { COMMUNITY_IMAGE_MAX_WIDTH, COMMUNITY_IMAGE_MIN_WIDTH } from '@/entities/community';

interface ResizeState {
  width: number;
  height: number;
}

function clampImageWidth(width: number, maxWidth = COMMUNITY_IMAGE_MAX_WIDTH) {
  return Math.min(maxWidth, Math.max(COMMUNITY_IMAGE_MIN_WIDTH, Math.round(width)));
}

function parseImageDimension(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return value;
  }

  if (typeof value !== 'string' || value.trim() === '') {
    return null;
  }

  const parsedValue = Number(value);
  return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : null;
}

function resolveAspectRatio(image: HTMLImageElement, width: number, height: number | null) {
  if (image.naturalWidth > 0 && image.naturalHeight > 0) {
    return image.naturalWidth / image.naturalHeight;
  }

  if (height != null && height > 0) {
    return width / height;
  }

  return 1;
}

function createImageSize(width: number, aspectRatio: number, maxWidth = COMMUNITY_IMAGE_MAX_WIDTH): ResizeState {
  const normalizedWidth = clampImageWidth(width, maxWidth);
  return {
    width: normalizedWidth,
    height: Math.max(1, Math.round(normalizedWidth / Math.max(aspectRatio, 0.01))),
  };
}

function resolveEditorContentWidth(image: HTMLImageElement) {
  const editorBody = image.closest('.community-editor-body, .community-detail-editor-body');
  if (editorBody instanceof HTMLElement && editorBody.clientWidth > 0) {
    return Math.max(COMMUNITY_IMAGE_MIN_WIDTH, Math.min(COMMUNITY_IMAGE_MAX_WIDTH, editorBody.clientWidth));
  }

  return COMMUNITY_IMAGE_MAX_WIDTH;
}

export function ResizableCommunityImage({ node, selected, updateAttributes }: ReactNodeViewProps) {
  const imageRef = useRef<HTMLImageElement | null>(null);
  const width = parseImageDimension(node.attrs.width);
  const height = parseImageDimension(node.attrs.height);
  const [previewSize, setPreviewSize] = useState<ResizeState | null>(null);
  const displayWidth = previewSize?.width ?? width ?? undefined;
  const displayHeight = previewSize?.height ?? height ?? undefined;
  const imageStyle = useMemo(
    () => ({
      width: displayWidth != null ? '100%' : undefined,
      height: 'auto',
      maxWidth: '100%',
    }),
    [displayWidth],
  );

  function handleImageLoad() {
    const image = imageRef.current;
    if (image == null || width != null || image.naturalWidth <= 0 || image.naturalHeight <= 0) {
      return;
    }

    const imageSize = createImageSize(
      image.naturalWidth,
      image.naturalWidth / image.naturalHeight,
      resolveEditorContentWidth(image),
    );
    updateAttributes(imageSize);
  }

  function handleResizeStart(event: PointerEvent<HTMLButtonElement>) {
    const image = imageRef.current;
    if (image == null) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();

    const startX = event.clientX;
    const startWidth = image.getBoundingClientRect().width;
    const aspectRatio = resolveAspectRatio(image, startWidth, height);
    const maxWidth = resolveEditorContentWidth(image);
    let nextSize = createImageSize(startWidth, aspectRatio, maxWidth);
    setPreviewSize(nextSize);

    function handlePointerMove(pointerEvent: globalThis.PointerEvent) {
      nextSize = createImageSize(startWidth + pointerEvent.clientX - startX, aspectRatio, maxWidth);
      setPreviewSize(nextSize);
    }

    function handlePointerUp() {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
      setPreviewSize(null);
      updateAttributes(nextSize);
    }

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp, { once: true });
  }

  return (
    <NodeViewWrapper
      as="figure"
      className={`community-resizable-image ${selected ? 'is-selected' : ''} ${previewSize != null ? 'is-resizing' : ''}`.trim()}
      style={{ width: displayWidth != null ? `min(${displayWidth}px, 100%)` : undefined }}
    >
      <img
        ref={imageRef}
        src={node.attrs.src}
        alt={node.attrs.alt ?? ''}
        title={node.attrs.title ?? undefined}
        width={displayWidth}
        height={displayHeight}
        className="community-content-image"
        style={imageStyle}
        draggable={false}
        onLoad={handleImageLoad}
      />
      <button
        type="button"
        className="community-image-resize-handle"
        contentEditable={false}
        aria-label="이미지 크기 조절"
        onPointerDown={handleResizeStart}
      />
      {previewSize != null ? (
        <span className="community-image-resize-label" contentEditable={false}>
          {previewSize.width}px
        </span>
      ) : null}
    </NodeViewWrapper>
  );
}
