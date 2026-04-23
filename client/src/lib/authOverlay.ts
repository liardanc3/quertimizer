export const OPEN_LOGIN_OVERLAY_EVENT = 'quertimizer-open-login-overlay';

export interface OpenLoginOverlayEventDetail {
  description?: string | null;
}

let currentLoginOverlayDescription: string | null = null;

function normalizeDescription(description?: string | null) {
  const normalizedDescription = description?.trim() ?? '';
  return normalizedDescription === '' ? null : normalizedDescription;
}

export function setLoginOverlayDescription(description?: string | null) {
  currentLoginOverlayDescription = normalizeDescription(description);
}

export function getLoginOverlayDescription() {
  return currentLoginOverlayDescription;
}

export function openLoginOverlay(description?: string | null) {
  const nextDescription = description === undefined
    ? currentLoginOverlayDescription
    : normalizeDescription(description);

  window.dispatchEvent(new CustomEvent<OpenLoginOverlayEventDetail>(OPEN_LOGIN_OVERLAY_EVENT, {
    detail: {
      description: nextDescription,
    },
  }));
}
