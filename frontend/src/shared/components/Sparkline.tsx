import type { SparklinePonto } from '../lib/types';

interface SparklineProps {
  pontos: SparklinePonto[];
  color: string;
}

// M23 — sem lib de gráfico (nenhuma está instalada no projeto de propósito) — polyline SVG
// simples, mesmo espírito do bar chart artesanal de "Crescimento de mentorados".
export function Sparkline({ pontos, color }: SparklineProps) {
  if (pontos.length === 0) {
    return null;
  }

  const largura = 100;
  const altura = 28;
  const maior = Math.max(1, ...pontos.map((p) => p.valor));
  const passo = pontos.length > 1 ? largura / (pontos.length - 1) : 0;
  const coordenadas = pontos
    .map((p, i) => `${i * passo},${altura - (p.valor / maior) * altura}`)
    .join(' ');

  return (
    <svg
      viewBox={`0 0 ${largura} ${altura}`}
      width="100%"
      height={altura}
      preserveAspectRatio="none"
      role="img"
      aria-label="Tendência dos últimos 6 meses"
    >
      <polyline points={coordenadas} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
