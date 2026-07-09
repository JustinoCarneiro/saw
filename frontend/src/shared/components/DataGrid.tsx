import type { ReactNode } from 'react';
import styles from './DataGrid.module.css';

interface DataGridProps {
  columns: string;
  headers: string[];
  children: ReactNode;
  lastColumnAlign?: 'left' | 'right';
}

// Colunas em `fr` puro crescem pra caber o conteúdo (min-content) antes de respeitar a
// proporção definida, o que pode estourar a largura do card. `minmax(0, Nfr)` trava a
// coluna na proporção real e deixa o conteúdo lidar com o espaço que sobrar (nowrap/ellipsis),
// em vez de forçar o grid inteiro a crescer.
function toGridTemplate(columns: string): string {
  return columns
    .split(/\s+/)
    .map((token) => (/^[\d.]+fr$/.test(token) ? `minmax(0, ${token})` : token))
    .join(' ');
}

export function DataGrid({ columns, headers, children, lastColumnAlign = 'left' }: DataGridProps) {
  return (
    <div className={styles.wrap}>
      <div className={styles.grid} style={{ gridTemplateColumns: toGridTemplate(columns) }}>
        {headers.map((h, i) => (
          <div
            key={h}
            className={styles.headerCell}
            style={i === headers.length - 1 && lastColumnAlign === 'right' ? { textAlign: 'right' } : undefined}
          >
            {h}
          </div>
        ))}
      </div>
      {children}
    </div>
  );
}

export function DataGridRow({ columns, children, testId }: { columns: string; children: ReactNode; testId?: string }) {
  return (
    <div
      className={`${styles.grid} ${styles.row}`}
      style={{ gridTemplateColumns: toGridTemplate(columns) }}
      data-testid={testId}
    >
      {children}
    </div>
  );
}
