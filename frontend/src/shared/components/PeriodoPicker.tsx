import { MESES_PT } from '../lib/format';
import { ICON_PROPS } from './iconProps';
import styles from './PeriodoPicker.module.css';

interface PeriodoPickerProps {
  ano: number;
  mes: number;
  onChange: (ano: number, mes: number) => void;
}

export function PeriodoPicker({ ano, mes, onChange }: PeriodoPickerProps) {
  const anoAtual = new Date().getFullYear();
  const anos = Array.from({ length: 5 }, (_, i) => anoAtual - i);

  return (
    <div className={styles.picker}>
      <svg {...ICON_PROPS} width={16} height={16}>
        <rect x="3" y="4" width="18" height="18" rx="2" />
        <line x1="16" y1="2" x2="16" y2="6" />
        <line x1="8" y1="2" x2="8" y2="6" />
        <line x1="3" y1="10" x2="21" y2="10" />
      </svg>
      <select className={styles.select} aria-label="Mês" value={mes} onChange={(e) => onChange(ano, Number(e.target.value))}>
        {MESES_PT.map((nome, i) => (
          <option key={nome} value={i + 1}>
            {nome}
          </option>
        ))}
      </select>
      <select className={styles.select} aria-label="Ano" value={ano} onChange={(e) => onChange(Number(e.target.value), mes)}>
        {anos.map((a) => (
          <option key={a} value={a}>
            {a}
          </option>
        ))}
      </select>
    </div>
  );
}
