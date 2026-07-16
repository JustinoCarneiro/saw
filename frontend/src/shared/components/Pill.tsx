import type { ReactNode } from 'react';
import styles from './Pill.module.css';

interface PillProps {
  children: ReactNode;
  bg: string;
  color: string;
}

export function Pill({ children, bg, color }: PillProps) {
  return (
    <span className={styles.pill} style={{ background: bg, color }}>
      {children}
    </span>
  );
}

const STATUS_TOKENS: Record<string, { bg: string; color: string; label: string }> = {
  EM_DIA: { bg: 'var(--success-bg)', color: 'var(--success)', label: 'Em dia' },
  ATENCAO: { bg: 'var(--warning-bg)', color: 'var(--warning)', label: 'Em atenção' },
  ATRASADO: { bg: 'var(--danger-bg)', color: 'var(--danger)', label: 'Atrasado' },
};

export function StatusPill({ status }: { status: string }) {
  const token = STATUS_TOKENS[status] ?? { bg: 'var(--line)', color: 'var(--text-soft)', label: status };
  return (
    <Pill bg={token.bg} color={token.color}>
      {token.label}
    </Pill>
  );
}

const AREA_TOKENS: Record<string, { bg: string; color: string; label: string; dot: string }> = {
  ADMIN: { bg: 'var(--warning-bg)', color: 'var(--warning)', label: 'Admin', dot: 'var(--gold)' },
  COMERCIAL: { bg: 'var(--info-bg)', color: 'var(--info)', label: 'Comercial', dot: 'var(--info)' },
  MARKETING: { bg: 'rgba(122,35,40,.22)', color: '#E08A8E', label: 'Marketing', dot: '#E08A8E' },
  GESTAO_PERFORMANCE: {
    bg: 'var(--success-bg)',
    color: 'var(--success)',
    label: 'Gestão de Performance',
    dot: 'var(--success)',
  },
};

export function AreaPill({ area }: { area: string }) {
  const token = AREA_TOKENS[area] ?? { bg: 'var(--line)', color: 'var(--text-soft)', label: area };
  return (
    <Pill bg={token.bg} color={token.color}>
      {token.label}
    </Pill>
  );
}

export function areaLabel(area: string): string {
  return AREA_TOKENS[area]?.label ?? area;
}

export function areaDotColor(area: string): string {
  return AREA_TOKENS[area]?.dot ?? 'var(--text-faint)';
}
