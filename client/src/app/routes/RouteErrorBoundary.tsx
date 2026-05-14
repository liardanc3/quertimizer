import { Component, type ErrorInfo, type ReactNode } from 'react';
import { PageLoadFailureState, PageLoading } from '@/shared/ui';

interface RouteErrorBoundaryProps {
  routeKey: string;
  children: ReactNode;
}

interface RouteErrorBoundaryState {
  routeKey: string;
  error: unknown;
  reloading: boolean;
}

const ROUTE_ASSET_RELOAD_PREFIX = 'quertimizer.route.asset.reload';

export default class RouteErrorBoundary extends Component<RouteErrorBoundaryProps, RouteErrorBoundaryState> {

  state: RouteErrorBoundaryState = {
    routeKey: this.props.routeKey,
    error: null,
    reloading: false,
  };

  static getDerivedStateFromProps(props: RouteErrorBoundaryProps, state: RouteErrorBoundaryState) {
    if (props.routeKey !== state.routeKey) {
      return { routeKey: props.routeKey, error: null, reloading: false };
    }

    return null;
  }

  static getDerivedStateFromError(error: unknown) {
    return { error, reloading: false };
  }

  componentDidCatch(error: unknown, errorInfo: ErrorInfo) {
    if (isRouteAssetLoadError(error) && reloadCurrentRouteOnce()) {
      this.setState({ reloading: true });
      window.location.reload();
      return;
    }

    console.error('Route render failed', error, errorInfo);
  }

  render() {
    if (this.state.reloading) {
      return <PageLoading />;
    }

    if (this.state.error != null) {
      return (
        <div className="page-stack route-inline-state-layout">
          <PageLoadFailureState
            className="route-inline-state-message"
            message="페이지를 불러오지 못했습니다. 새로고침 후 다시 시도해 주세요."
          />
        </div>
      );
    }

    return this.props.children;
  }
}

function isRouteAssetLoadError(error: unknown) {
  // lazy import와 CSS chunk 로딩 실패 여부 확인
  const name = error instanceof Error ? error.name : '';
  const message = error instanceof Error ? error.message : String(error);
  return [
    name,
    message,
  ].some((value) =>
    /ChunkLoadError|CSS_CHUNK_LOAD_FAILED|Loading chunk|dynamically imported module|module script failed|Failed to fetch/i.test(value)
  );
}

function reloadCurrentRouteOnce() {
  // 현재 앱 번들과 경로 기준 자동 새로고침 1회 제한
  const reloadKey = buildRouteAssetReloadKey();
  if (window.sessionStorage.getItem(reloadKey) === 'true') {
    return false;
  }

  window.sessionStorage.setItem(reloadKey, 'true');
  return true;
}

function buildRouteAssetReloadKey() {
  // 현재 로드된 entry script까지 포함해 다음 배포 때는 다시 복구 가능하게 구분
  const entryScript = Array.from(document.scripts)
    .map((script) => script.src)
    .find((src) => src.includes('/assets/index-')) ?? 'dev';
  return `${ROUTE_ASSET_RELOAD_PREFIX}:${entryScript}:${window.location.pathname}`;
}
