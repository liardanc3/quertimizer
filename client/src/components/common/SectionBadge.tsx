interface SectionBadgeProps {
  label: string;
  disabled?: boolean;
}

export default function SectionBadge({ label, disabled = false }: SectionBadgeProps) {
  return <span className={`section-badge ${disabled ? 'is-disabled' : ''}`}>{label}</span>;
}
