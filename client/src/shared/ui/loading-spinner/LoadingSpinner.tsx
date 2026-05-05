import { useUiText } from '@/shared/config/ui-text';

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

export function PageLoading({ className = '', label }: Omit<ContentLoadingProps, 'as'>) {
  return <ContentLoading as="section" className={className} label={label} />;
}

export function SectionLoading({ className = '', label }: ContentLoadingProps) {
  return <ContentLoading as="div" className={className} label={label} />;
}

export function TableLoadingOverlay(props: LoadingOverlayProps) {
  return <LoadingOverlay {...props} />;
}

export function InlineSpinner({ className = '', label }: Omit<LoadingOverlayProps, 'ariaHidden'>) {
  const { text } = useUiText();
  return (
    <span className={getClassName('inline-spinner', className)} aria-live="polite" aria-label={label ?? text('COMMON_LOADING_STATUS', '로딩 중')} />
  );
}

export default ContentLoading;
