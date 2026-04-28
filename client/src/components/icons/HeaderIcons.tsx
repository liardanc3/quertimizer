export function AlarmListIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M4.5 5.4h11M4.5 10h11M4.5 14.6h11" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
    </svg>
  );
}

export function MenuIcon({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      {open ? (
        <>
          <path d="M5 5 15 15" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M15 5 5 15" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </>
      ) : (
        <>
          <path d="M4.25 5.6h11.5" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M4.25 10h11.5" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M4.25 14.4h11.5" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </>
      )}
    </svg>
  );
}

export function BellIcon() {
  return (
    <svg className="header-notification-icon" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M12 3.75a4.25 4.25 0 0 0-4.25 4.25v1.14c0 .9-.28 1.77-.8 2.5l-1.27 1.79a1.75 1.75 0 0 0 1.43 2.77h9.78a1.75 1.75 0 0 0 1.43-2.77l-1.27-1.79a4.3 4.3 0 0 1-.8-2.5V8A4.25 4.25 0 0 0 12 3.75Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M9.75 18.25a2.25 2.25 0 0 0 4.5 0"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  );
}
