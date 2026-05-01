import type { CommunityPostCategory } from '../types/domain';

const COMMUNITY_STORAGE_KEY = 'quertimizer.community.state';
const COMMUNITY_CHANGE_EVENT = 'quertimizer:community-change';

export interface CommunityEditorDraft {
  title: string;
  category?: Extract<CommunityPostCategory, 'discussion' | 'question' | 'notice'>;
  draftTag: string;
  selectedTags: string[];
  contentJson: string;
  contentHtml?: string;
  updatedAt: string;
}

interface CommunityStoreState {
  drafts: Record<string, CommunityEditorDraft>;
}

const defaultState: CommunityStoreState = {
  drafts: {},
};

function emitCommunityChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(COMMUNITY_CHANGE_EVENT));
}

function sanitizeState(value: unknown): CommunityStoreState {
  if (!value || typeof value !== 'object') {
    return defaultState;
  }

  const state = value as Partial<CommunityStoreState>;
  return {
    drafts: state.drafts && typeof state.drafts === 'object' ? state.drafts : {},
  };
}

function readCommunityState() {
  if (typeof window === 'undefined') {
    return defaultState;
  }

  try {
    const rawValue = window.localStorage.getItem(COMMUNITY_STORAGE_KEY);
    return rawValue ? sanitizeState(JSON.parse(rawValue)) : defaultState;
  } catch {
    return defaultState;
  }
}

function writeCommunityState(nextState: CommunityStoreState) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(COMMUNITY_STORAGE_KEY, JSON.stringify(nextState));
  emitCommunityChange();
}

export function subscribeCommunityStore(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(COMMUNITY_CHANGE_EVENT, callback);
  window.addEventListener('storage', callback);

  return () => {
    window.removeEventListener(COMMUNITY_CHANGE_EVENT, callback);
    window.removeEventListener('storage', callback);
  };
}

export function getCommunityStoreSnapshot() {
  if (typeof window === 'undefined') {
    return '';
  }

  return window.localStorage.getItem(COMMUNITY_STORAGE_KEY) ?? '';
}

export function getCommunityEditorDraft(draftKey: string) {
  return readCommunityState().drafts[draftKey];
}

export function saveCommunityEditorDraft(draftKey: string, draft: Omit<CommunityEditorDraft, 'updatedAt'>) {
  const state = readCommunityState();

  writeCommunityState({
    ...state,
    drafts: {
      ...state.drafts,
      [draftKey]: {
        ...draft,
        updatedAt: new Date().toISOString(),
      },
    },
  });
}

export function clearCommunityEditorDraft(draftKey: string) {
  const state = readCommunityState();

  if (!state.drafts[draftKey]) {
    return;
  }

  const restDrafts = { ...state.drafts };
  delete restDrafts[draftKey];

  writeCommunityState({
    ...state,
    drafts: restDrafts,
  });
}
