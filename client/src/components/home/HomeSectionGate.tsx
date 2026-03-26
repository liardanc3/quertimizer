import type { HomeSectionType } from '../../types/domain';

interface HomeSectionGateProps {
  selectedSection: HomeSectionType;
  onChange: (section: HomeSectionType) => void;
}

const sections = [
  { id: 'ranking' as const, label: '랭킹', disabled: true },
  { id: 'problems' as const, label: '문제', disabled: false },
];

export default function HomeSectionGate({ selectedSection, onChange }: HomeSectionGateProps) {
  return (
    <div className="section-gate">
      {sections.map((section) => (
        <button
          key={section.id}
          type="button"
          onClick={() => onChange(section.id)}
          disabled={section.disabled}
          className={`mini-toggle ${selectedSection === section.id ? 'is-selected' : ''}`}
        >
          {section.label}
          {section.disabled && <span className="tab-meta">예정</span>}
        </button>
      ))}
    </div>
  );
}
