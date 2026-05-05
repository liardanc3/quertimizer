import type { ReactNode } from 'react';
import { Footer } from '@/widgets/footer';
import { Header } from '@/widgets/header';

interface MainLayoutProps {
  children: ReactNode;
}

export default function MainLayout({ children }: MainLayoutProps) {
  return (
    <div className="app-shell">
      <Header />
      <main className="main-content">{children}</main>
      <Footer />
    </div>
  );
}
