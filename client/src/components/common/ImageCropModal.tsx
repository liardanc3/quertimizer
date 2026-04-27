import { useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import Cropper, { type Area } from 'react-easy-crop';
import type { ImageCropAreaPixels } from '../../lib/imageCrop';
import { useUiText } from '../../lib/uiText';
import './ImageCropModal.css';
import 'react-easy-crop/react-easy-crop.css';

interface ImageCropModalProps {
  ariaLabel: string;
  imageSrc: string;
  aspect: number;
  cropShape?: 'rect' | 'round';
  objectFit?: 'contain' | 'cover' | 'horizontal-cover' | 'vertical-cover';
  minZoom?: number;
  maxZoom?: number;
  initialZoom?: number;
  isApplying?: boolean;
  onCancel: () => void;
  onApply: (areaPixels: ImageCropAreaPixels) => void | Promise<void>;
}

export default function ImageCropModal({
  ariaLabel,
  imageSrc,
  aspect,
  cropShape = 'rect',
  objectFit = 'contain',
  minZoom = 1,
  maxZoom = 3,
  initialZoom = 1,
  isApplying = false,
  onCancel,
  onApply,
}: ImageCropModalProps) {
  const { text } = useUiText();
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(initialZoom);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<ImageCropAreaPixels | null>(null);

  if (typeof document === 'undefined') {
    return null;
  }

  const modal = (
    <div className="image-crop-modal-overlay" role="presentation">
      <section className={`image-crop-modal ${isApplying ? 'is-applying' : ''}`.trim()} role="dialog" aria-modal="true" aria-label={ariaLabel}>
        <div className="image-crop-modal-body">
          <div className="image-crop-canvas-shell">
            <Cropper
              image={imageSrc}
              crop={crop}
              zoom={zoom}
              aspect={aspect}
              cropShape={cropShape}
              objectFit={objectFit}
              minZoom={minZoom}
              maxZoom={maxZoom}
              showGrid={false}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={(_, areaPixels: Area) => setCroppedAreaPixels(areaPixels)}
            />
          </div>

          <div className="image-crop-modal-controls">
            <label className="image-crop-zoom-field">
              <span>{text('IMAGE_CROP_ZOOM_LABEL', '확대')}</span>
              <input
                type="range"
                min={String(minZoom)}
                max={String(maxZoom)}
                step="0.01"
                value={zoom}
                onChange={(event) => setZoom(Number.parseFloat(event.target.value))}
              />
            </label>
          </div>
        </div>

        <div className="image-crop-modal-actions">
          <button type="button" className="btn ghost" onClick={onCancel} disabled={isApplying}>
            {text('COMMON_CANCEL_BUTTON', '취소')}
          </button>
          <button
            type="button"
            className="btn primary"
            onClick={() => {
              if (croppedAreaPixels) {
                void onApply(croppedAreaPixels);
              }
            }}
            disabled={croppedAreaPixels == null || isApplying}
          >
            {isApplying ? text('COMMON_APPLYING_LABEL', '적용 중') : text('COMMON_APPLY_BUTTON', '적용')}
          </button>
        </div>

        {isApplying ? (
          <div className="image-crop-modal-progress-overlay" aria-hidden="true">
            <span className="image-crop-modal-loading-spinner" />
          </div>
        ) : null}
      </section>
    </div>
  );

  return createPortal(modal as ReactNode, document.body);
}
