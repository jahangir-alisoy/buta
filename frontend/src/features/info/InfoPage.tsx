import type { ReactNode } from 'react';

export function InfoPage({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="container page" style={{ maxWidth: 760 }}>
      <header className="page-header">
        <h1 className="page-title">{title}</h1>
      </header>
      <div className="stack muted">{children}</div>
    </div>
  );
}
