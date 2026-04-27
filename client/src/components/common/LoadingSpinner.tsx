import { useUiText } from '../../lib/uiText';

type LoadingElement = 'div' | 'section';

interface ContentLoadingProps {
  as?: LoadingElement;
  className?: string;
  label?: string;
}

interface LoadingOverlayProps {
  className?: string;
  label?: string;
  ariaHidden?: boolean;
}

function getClassName(baseClassName: string, className: string) {
  return `${baseClassName} ${className}`.trim();
}

export function ContentLoading({ as = 'div', className = '', label }: ContentLoadingProps) {
  const { text } = useUiText();
  const resolvedLabel = label ?? text('COMMON_LOADING_STATUS', '로딩 중');
  const loadingProps = {
    className: getClassName('page-loading-shell content-loading-shell', className),
    'aria-live': 'polite' as const,
    'aria-label': resolvedLabel,
    'aria-busy': true,
  };

  return as === 'section' ? <section {...loadingProps} /> : <div {...loadingProps} />;
}

export function LoadingOverlay({ className = '', label, ariaHidden = false }: LoadingOverlayProps) {
  const { text } = useUiText();
  const overlayClassName = getClassName('submit-history-loading-overlay', className);

  if (ariaHidden) {
    return <div className={overlayClassName} aria-hidden="true" />;
  }

  return <div className={overlayClassName} aria-live="polite" aria-label={label ?? text('COMMON_LOADING_STATUS', '로딩 중')} />;
}

export default ContentLoading;
