import type { FormEventHandler, ReactNode } from 'react';

interface PageToolbarProps {
  className?: string;
  children: ReactNode;
}

interface SegmentedTabsProps<T extends string> {
  className?: string;
  label: string;
  tabs: Array<{ value: T; label: string; disabled?: boolean; title?: string }>;
  selectedValue: T;
  onSelect: (value: T) => void;
  actions?: ReactNode;
}

interface SearchFormProps {
  className?: string;
  fieldClassName?: string;
  inputClassName?: string;
  buttonClassName?: string;
  value: string;
  placeholder: string;
  label: string;
  submitLabel: string;
  buttonLabel: string;
  withIcon?: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
}

export function PageToolbar({ className = '', children }: PageToolbarProps) {
  return <div className={`page-toolbar ${className}`.trim()}>{children}</div>;
}

export function SegmentedTabs<T extends string>({
  className = '',
  label,
  tabs,
  selectedValue,
  onSelect,
  actions,
}: SegmentedTabsProps<T>) {
  return (
    <div className={`tab-row ${className}`.trim()} role="tablist" aria-label={label}>
      {tabs.map((tab) => {
        const isSelected = tab.value === selectedValue;

        return (
          <button
            key={tab.value}
            type="button"
            className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''} ${tab.disabled ? 'is-disabled' : ''}`.trim()}
            role="tab"
            aria-selected={isSelected}
            disabled={tab.disabled}
            title={tab.title}
            onClick={() => {
              if (!isSelected && !tab.disabled) {
                onSelect(tab.value);
              }
            }}
          >
            {tab.label}
          </button>
        );
      })}
      {actions}
    </div>
  );
}

export function SearchForm({
  className = '',
  fieldClassName = '',
  inputClassName = '',
  buttonClassName = '',
  value,
  placeholder,
  label,
  submitLabel,
  buttonLabel,
  withIcon = false,
  onChange,
  onSubmit,
}: SearchFormProps) {
  const handleSubmit: FormEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    onSubmit();
  };

  return (
    <form className={`search-form ${className}`.trim()} onSubmit={handleSubmit}>
      <label className={`search-field ${fieldClassName}`.trim()}>
        {withIcon ? (
          <span className="problem-search-icon" aria-hidden="true">
            ⌕
          </span>
        ) : null}
        <input
          type="search"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className={`text-field ${inputClassName}`.trim()}
          placeholder={placeholder}
          aria-label={label}
        />

        <button type="submit" className={buttonClassName} aria-label={submitLabel}>
          {buttonLabel}
        </button>
      </label>
    </form>
  );
}
