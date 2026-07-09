import type { CSSProperties, ReactNode } from 'react';
import styles from './Card.module.css';

interface CardProps {
  children: ReactNode;
  style?: CSSProperties;
  className?: string;
  testId?: string;
}

export function Card({ children, style, className, testId }: CardProps) {
  return (
    <div className={[styles.card, className].filter(Boolean).join(' ')} style={style} data-testid={testId}>
      {children}
    </div>
  );
}
