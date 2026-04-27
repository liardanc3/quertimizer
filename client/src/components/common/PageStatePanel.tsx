import './PageStatePanel.css';

interface PageStatePanelProps {
  label: string;
  title: string;
  description: string;
  actionLabel?: string;
  actionTone?: 'primary' | 'ghost' | 'text';
  className?: string;
  fullPage?: boolean;
  onAction?: () => void;
}

export default function PageStatePanel({
  label,
  title,
  description,
  actionLabel,
  actionTone = 'primary',
  className = '',
  fullPage = false,
  onAction,
}: PageStatePanelProps) {
  const panel = (
    <section className={`panel-card page-state-panel ${className}`.trim()} role="status">
      <div className="page-state-copy">
        <p className="panel-meta">{label}</p>
        <h1 className="page-title">{title}</h1>
        <p className="muted-text">{description}</p>
      </div>

      {actionLabel && onAction ? (
        <div className="page-state-actions">
          <button type="button" className={`btn ${actionTone}`} onClick={onAction}>
            {actionLabel}
          </button>
        </div>
      ) : null}
    </section>
  );

  if (!fullPage) {
    return panel;
  }

  return <div className="page-stack page-state-layout">{panel}</div>;
}
