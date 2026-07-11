interface LineChartPonto {
  chave: string;
  rotulo: string;
  valor: number;
}

interface LineChartProps {
  pontos: LineChartPonto[];
  color?: string;
}

function tetoAgradavel(valor: number): number {
  if (valor <= 5) return 5;
  const grandeza = Math.pow(10, Math.floor(Math.log10(valor)));
  const passo = grandeza / 2 || 1;
  return Math.ceil(valor / passo) * passo;
}

function formatarValorEixo(v: number): string {
  if (v >= 1000) {
    const emMil = v / 1000;
    return `${Number.isInteger(emMil) ? emMil : emMil.toFixed(1)}K`;
  }
  return String(Math.round(v));
}

// Réplica do gráfico de linha do mockup (design/mockups-ref/06-admin.png, Tela 11 —
// "Crescimento de mentorados"): eixo Y com 4 marcações, linha com marcadores e área em
// gradiente sob a curva. Sem lib de gráfico — SVG puro, mesmo espírito do resto do sistema.
export function LineChart({ pontos, color = 'var(--gold)' }: LineChartProps) {
  if (pontos.length === 0) {
    return null;
  }

  const largura = 600;
  const altura = 220;
  const margemEsquerda = 40;
  const margemBaixo = 24;
  const margemTopo = 8;
  const plotLargura = largura - margemEsquerda - 8;
  const plotAltura = altura - margemBaixo - margemTopo;

  const maiorValor = Math.max(...pontos.map((p) => p.valor));
  const teto = tetoAgradavel(maiorValor);
  const gridValores = [0, teto / 3, (teto * 2) / 3, teto];

  const passo = pontos.length > 1 ? plotLargura / (pontos.length - 1) : 0;
  const coords = pontos.map((p, i) => ({
    x: margemEsquerda + i * passo,
    y: margemTopo + plotAltura - (teto === 0 ? 0 : (p.valor / teto) * plotAltura),
  }));

  const linePath = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
  const areaPath = `${linePath} L ${coords[coords.length - 1].x.toFixed(1)} ${(margemTopo + plotAltura).toFixed(1)} `
    + `L ${coords[0].x.toFixed(1)} ${(margemTopo + plotAltura).toFixed(1)} Z`;

  return (
    <svg viewBox={`0 0 ${largura} ${altura}`} width="100%" height={altura} role="img" aria-label="Crescimento nos últimos 6 meses">
      <defs>
        <linearGradient id="lineChartArea" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.25" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>

      {gridValores.map((v) => {
        const y = margemTopo + plotAltura - (teto === 0 ? 0 : (v / teto) * plotAltura);
        return (
          <g key={v}>
            <line x1={margemEsquerda} y1={y} x2={largura - 8} y2={y} stroke="var(--line)" strokeWidth="1" />
            <text x={margemEsquerda - 8} y={y + 3.5} fontSize="10" fill="var(--text-faint)" textAnchor="end">
              {formatarValorEixo(v)}
            </text>
          </g>
        );
      })}

      <path d={areaPath} fill="url(#lineChartArea)" />
      <path d={linePath} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      {coords.map((c, i) => (
        <circle key={pontos[i].chave} cx={c.x} cy={c.y} r="3.5" fill={color} />
      ))}

      {coords.map((c, i) => (
        <text key={pontos[i].chave} x={c.x} y={altura - 6} fontSize="10" fill="var(--text-faint)" textAnchor="middle">
          {pontos[i].rotulo}
        </text>
      ))}
    </svg>
  );
}
