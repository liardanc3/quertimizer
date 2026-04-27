import { getUiTextValue } from './uiText';

export interface ImageCropAreaPixels {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface CreateCroppedImageFileOptions {
  file: File;
  cropAreaPixels: ImageCropAreaPixels;
  outputWidth: number;
  outputHeight: number;
  fileName: string;
  quality?: number;
}

interface LoadedCropSource {
  source: CanvasImageSource;
  width: number;
  height: number;
  release: () => void;
}

export async function createCroppedImageFile(options: CreateCroppedImageFileOptions) {
  const loadedSource = await loadCropSource(options.file);

  try {
    await waitForNextFrame();

    const canvas = document.createElement('canvas');
    canvas.width = options.outputWidth;
    canvas.height = options.outputHeight;

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error(getUiTextValue('IMAGE_CROP_CANVAS_FAIL_MESSAGE', '이미지 편집용 캔버스를 생성할 수 없습니다.'));
    }

    context.imageSmoothingEnabled = true;
    context.imageSmoothingQuality = 'high';
    context.clearRect(0, 0, canvas.width, canvas.height);

    const sourceX = clamp(options.cropAreaPixels.x, 0, loadedSource.width);
    const sourceY = clamp(options.cropAreaPixels.y, 0, loadedSource.height);
    const sourceWidth = clamp(options.cropAreaPixels.width, 1, loadedSource.width - sourceX);
    const sourceHeight = clamp(options.cropAreaPixels.height, 1, loadedSource.height - sourceY);

    context.drawImage(
      loadedSource.source,
      sourceX,
      sourceY,
      sourceWidth,
      sourceHeight,
      0,
      0,
      options.outputWidth,
      options.outputHeight,
    );

    const webpBlob = await canvasToBlob(canvas, 'image/webp', options.quality ?? 0.98);
    if (webpBlob) {
      return new File([webpBlob], `${options.fileName}.webp`, {
        type: 'image/webp',
        lastModified: Date.now(),
      });
    }

    const pngBlob = await canvasToBlob(canvas, 'image/png', 0.96);
    if (!pngBlob) {
      throw new Error(getUiTextValue('IMAGE_CROP_CONVERT_FAIL_MESSAGE', '이미지를 변환하지 못했습니다.'));
    }

    return new File([pngBlob], `${options.fileName}.png`, {
      type: 'image/png',
      lastModified: Date.now(),
    });
  } finally {
    loadedSource.release();
  }
}

async function loadCropSource(file: File): Promise<LoadedCropSource> {
  if ('createImageBitmap' in window) {
    const bitmap = await createImageBitmap(file);
    return {
      source: bitmap,
      width: bitmap.width,
      height: bitmap.height,
      release: () => bitmap.close(),
    };
  }

  const objectUrl = URL.createObjectURL(file);
  const image = new Image();
  image.decoding = 'async';
  image.src = objectUrl;

  await new Promise<void>((resolve, reject) => {
    image.onload = () => resolve();
    image.onerror = () => reject(new Error(getUiTextValue('IMAGE_CROP_LOAD_FAIL_MESSAGE', '이미지를 불러오지 못했습니다.')));
  });

  return {
    source: image,
    width: image.naturalWidth,
    height: image.naturalHeight,
    release: () => URL.revokeObjectURL(objectUrl),
  };
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number) {
  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob((blob) => resolve(blob), type, quality);
  });
}

function waitForNextFrame() {
  return new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve());
  });
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}
