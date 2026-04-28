import { useSyncExternalStore } from 'react';
import { subscribeLocation } from '../lib/navigation';

export function getLocationPathnameSnapshot() {
  return window.location.pathname;
}

export function useLocationPathname() {
  return useSyncExternalStore(subscribeLocation, getLocationPathnameSnapshot, () => '/');
}

export function getLocationSearchSnapshot() {
  return window.location.search;
}

export function useLocationSearch() {
  return useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
}

export function replaceQueryState(path: string, state: unknown = window.history.state ?? {}) {
  const currentPath = `${window.location.pathname}${window.location.search}`;

  if (currentPath !== path) {
    window.history.replaceState(state, '', path);
  }
}

export function useQueryState() {
  const search = useLocationSearch();
  return new URLSearchParams(search);
}
