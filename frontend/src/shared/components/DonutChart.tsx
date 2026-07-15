import type { ReactNode } from 'react';

interface DonutSegmento {
  chave: string;
  valor: number;
  cor: string;
}

interface DonutChartProps {
  segmentos: DonutSegmento[];
  tamanho?: number;
  titulo?: string;
  // Conteúdo sobreposto ao centro do anel (ex.: "82%") — dashboard mentorado/E2 usa isto pro
  // indicador de "Evolução geral". Ausente por padrão pra não afetar quem já usa o componente
  // sem centro (Painel Consolidado/E17).
  centroConteudo?: ReactNode;
}

function pontoNoAnel(centro: number, raio: number, anguloDeg: number): { x: number; y: number } {
  const anguloRad = ((anguloDeg - 90) * Math.PI) / 180;
  return { x: centro + raio * Math.cos(anguloRad), y: centro + raio * Math.sin(anguloRad) };
}

// Arco via <path> (comando A), não <circle>+stroke-dasharray: a técnica de dasharray empilhado
// (offset negativo acumulado por segmento) se mostrou frágil com dados bem desbalanceados — ver
// M23, "distribuição por status" com 44/2/2 renderizava um vão visível entre segmentos, mesmo
// com os números do dasharray/offset conferindo na aritmética (achado do Marcos, print em mão).
// Path com ângulo início/fim explícito não depende de acúmulo de ponto flutuante ao longo da
// sequência, então não sofre desse desalinhamento.
function arco(centro: number, raio: number, anguloInicioDeg: number, anguloFimDeg: number): string {
  const inicio = pontoNoAnel(centro, raio, anguloInicioDeg);
  const fim = pontoNoAnel(centro, raio, anguloFimDeg);
  const largeArcFlag = anguloFimDeg - anguloInicioDeg <= 180 ? 0 : 1;
  // sweep-flag 1 = sentido do ângulo crescente, que aqui é horário (ver pontoNoAnel).
  return `M ${inicio.x} ${inicio.y} A ${raio} ${raio} 0 ${largeArcFlag} 1 ${fim.x} ${fim.y}`;
}

// Réplica do donut do mockup (design/mockups-ref/06-admin.png, Tela 11 — "Distribuição por
// plano"): anel de arcos SVG, sem lib de gráfico. Legenda é renderizada à parte por quem usa o
// componente (mesma divisão de responsabilidade do restante do sistema).
export function DonutChart({ segmentos, tamanho = 140, titulo = 'Distribuição por plano', centroConteudo }: DonutChartProps) {
  const total = segmentos.reduce((soma, s) => soma + s.valor, 0);
  // Proporcional a `tamanho` (não fixo): com valores fixos (raio=46, espessura=20) calibrados
  // pro tamanho=140 do Dashboard, um tamanho menor (ex.: 104 no Painel Consolidado) deixava
  // raio+espessura/2 (56) MAIOR que o próprio centro (52) — a borda externa do anel estourava
  // o viewBox e era cortada pelo overflow:hidden padrão do <svg>, aparecendo como um "quadrado"
  // em vez de círculo (achado do Marcos, reproduzido e confirmado por print).
  const raio = tamanho * (46 / 140);
  const espessura = tamanho * (20 / 140);
  const centro = tamanho / 2;

  let anguloAcumulado = 0;
  const visiveis = segmentos.filter((s) => s.valor > 0);

  return (
    <div style={{ position: 'relative', width: tamanho, height: tamanho }}>
      <svg viewBox={`0 0 ${tamanho} ${tamanho}`} width={tamanho} height={tamanho} role="img" aria-label={titulo}>
        {total === 0 ? (
          <circle cx={centro} cy={centro} r={raio} fill="none" stroke="var(--line)" strokeWidth={espessura} />
        ) : (
          visiveis.map((s) => {
            // Segmento único cobrindo 100%: o comando de arco degenera quando início e fim
            // coincidem, então desenha como anel fechado normal em vez de path.
            if (visiveis.length === 1) {
              return <circle key={s.chave} cx={centro} cy={centro} r={raio} fill="none" stroke={s.cor} strokeWidth={espessura} />;
            }
            const anguloInicio = anguloAcumulado;
            const anguloFim = anguloAcumulado + (s.valor / total) * 360;
            anguloAcumulado = anguloFim;
            return (
              <path
                key={s.chave}
                d={arco(centro, raio, anguloInicio, anguloFim)}
                fill="none"
                stroke={s.cor}
                strokeWidth={espessura}
              />
            );
          })
        )}
      </svg>
      {centroConteudo && (
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {centroConteudo}
        </div>
      )}
    </div>
  );
}
