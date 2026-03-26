import type { ReactNode } from 'react';
import Header from '../components/common/Header';

interface MainLayoutProps {
  children: ReactNode;
}

export default function MainLayout({ children }: MainLayoutProps) {
  return (
    <div className="app-shell">
      <Header />
      <main className="main-content">{children}</main>
    </div>
  );
}
