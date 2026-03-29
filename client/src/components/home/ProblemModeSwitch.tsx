interface ProblemModeSwitchProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  className?: string;
}

export default function ProblemModeSwitch({ label, checked, onChange, className = '' }: ProblemModeSwitchProps) {
  return (
    <label className={`problem-control-group problem-tag-control ${className}`.trim()}>
      <span className="problem-control-label">{label}</span>
      <span className="problem-checkbox-wrap">
        <input
          type="checkbox"
          checked={checked}
          onChange={(event) => onChange(event.target.checked)}
          className="problem-checkbox-input"
          aria-label={label}
        />
        <span className="problem-checkbox-ui" aria-hidden="true" />
      </span>
    </label>
  );
}
