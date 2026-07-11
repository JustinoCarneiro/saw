interface DonutSegmento {
  chave: string;
  valor: number;
  cor: string;
}

interface DonutChartProps {
  segmentos: DonutSegmento[];
  tamanho?: number;
}

// Réplica do donut do mockup (design/mockups-ref/06-admin.png, Tela 11 — "Distribuição por
// plano"): anel de arcos via stroke-dasharray, sem lib de gráfico. Legenda é renderizada à
// parte por quem usa o componente (mesma divisão de responsabilidade do restante do sistema).
export function DonutChart({ segmentos, tamanho = 140 }: DonutChartProps) {
  const total = segmentos.reduce((soma, s) => soma + s.valor, 0);
  const raio = 46;
  const espessura = 20;
  const circunferencia = 2 * Math.PI * raio;
  const centro = tamanho / 2;

  let acumulado = 0;

  return (
    <svg viewBox={`0 0 ${tamanho} ${tamanho}`} width={tamanho} height={tamanho} role="img" aria-label="Distribuição por plano">
      <g transform={`rotate(-90 ${centro} ${centro})`}>
        {total === 0 ? (
          <circle cx={centro} cy={centro} r={raio} fill="none" stroke="var(--line)" strokeWidth={espessura} />
        ) : (
          segmentos
            .filter((s) => s.valor > 0)
            .map((s) => {
              const fracao = s.valor / total;
              const comprimento = fracao * circunferencia;
              const offset = -acumulado * circunferencia;
              acumulado += fracao;
              return (
                <circle
                  key={s.chave}
                  cx={centro}
                  cy={centro}
                  r={raio}
                  fill="none"
                  stroke={s.cor}
                  strokeWidth={espessura}
                  strokeDasharray={`${comprimento} ${circunferencia - comprimento}`}
                  strokeDashoffset={offset}
                />
              );
            })
        )}
      </g>
    </svg>
  );
}
