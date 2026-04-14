export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="app-footer-inner">
        <div className="footer-bottom">
          <a
            className="footer-social-link"
            href="https://github.com/liardanc3"
            target="_blank"
            rel="noreferrer"
            aria-label="GitHub profile"
            title="GitHub"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 2C6.48 2 2 6.59 2 12.26c0 4.54 2.87 8.39 6.84 9.75.5.1.68-.22.68-.49 0-.24-.01-1.03-.01-1.87-2.78.62-3.37-1.22-3.37-1.22-.46-1.2-1.11-1.52-1.11-1.52-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.89 1.57 2.34 1.12 2.91.86.09-.66.35-1.12.63-1.37-2.22-.26-4.56-1.15-4.56-5.13 0-1.13.39-2.05 1.03-2.77-.1-.26-.45-1.31.1-2.74 0 0 .84-.28 2.75 1.06A9.3 9.3 0 0 1 12 6.84c.85 0 1.71.12 2.51.37 1.91-1.34 2.75-1.06 2.75-1.06.55 1.43.2 2.48.1 2.74.64.72 1.03 1.64 1.03 2.77 0 3.99-2.34 4.86-4.57 5.12.36.32.68.94.68 1.9 0 1.37-.01 2.48-.01 2.82 0 .27.18.6.69.49A10.28 10.28 0 0 0 22 12.26C22 6.59 17.52 2 12 2Z"
                fill="currentColor"
              />
            </svg>
          </a>
          <p className="footer-bottom-copy">&copy; 2026 Kim Dong Hwan. All rights reserved.</p>
          <p className="footer-bottom-copy footer-bottom-copy-secondary">
            Built with React &amp; Spring Boot, Hosted on Google Cloud Platform
          </p>
        </div>
      </div>
    </footer>
  );
}
