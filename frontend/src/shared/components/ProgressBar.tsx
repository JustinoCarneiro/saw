import styles from './ProgressBar.module.css';

interface ProgressBarProps {
  pct: number;
  color?: string;
}

export function ProgressBar({ pct, color = 'var(--gold)' }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, pct));
  return (
    <div className={styles.track}>
      <div className={styles.fill} style={{ width: `${clamped}%`, background: color }} />
    </div>
  );
}
