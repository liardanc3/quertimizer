import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@/app/styles/index.css';
import '@/shared/ui/styles/problem-list-page.css';
import '@/shared/ui/styles/runtime-panel.css';
import '@/shared/ui/styles/submit-history-page.css';
import App from '@/app/App';
import { installCsrfFetchInterceptor } from '@/shared/api/csrf-fetch';

installCsrfFetchInterceptor();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
