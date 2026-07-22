import type { CSSProperties } from 'react';

export const HORAS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
export const MINUTOS = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'));

// Achado de UX (22/07/2026, pedido do Marcos): input[type=datetime-local]/type=time exibem
// AM/PM ou 24h dependendo do idioma configurado no NAVEGADOR (Chromium ignora <html lang> pra
// isso — é ajuste do browser/SO, fora do nosso controle, confirmado ao vivo trocando lang="en"
// pra "pt-BR" no index.html e o seletor continuando em AM/PM). Combina <input type="date"> (sem
// ambiguidade de AM/PM) com dois <select> de hora (00-23) e minuto (00-59) que a gente controla
// por completo — garante 24h sempre, em qualquer navegador/SO. Mesmo formato de valor de um
// datetime-local ("YYYY-MM-DDTHH:mm"), então quem já lia/escrevia esse formato não precisa mudar.
export function DataHoraInput({ value, onChange, required, inputClassName, selectClassName }: {
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  inputClassName?: string;
  selectClassName?: string;
}) {
  const [data, hora] = value ? value.split('T') : ['', ''];
  const [h, m] = hora ? hora.split(':') : ['', ''];

  // Combina sempre os três pedaços, mesmo incompleto (ex.: só a data preenchida ainda) — early
  // return pra vazio aqui apagava o que já tinha sido digitado nos outros campos a cada troca
  // (achado ao vivo: preencher a data sozinha resetava o próprio input de data de volta pra
  // vazio, porque hora/minuto ainda estavam ""). A validação de completo/incompleto já é feita
  // pelo required nativo de cada controle — não precisa duplicar aqui.
  function atualizar(novaData: string, novaHora: string, novoMinuto: string) {
    if (!novaData && !novaHora && !novoMinuto) {
      onChange('');
      return;
    }
    onChange(`${novaData}T${novaHora}:${novoMinuto}`);
  }

  const legendaStyle: CSSProperties = {
    fontSize: 10.5,
    color: 'var(--text-soft)',
    marginBottom: 3,
    textTransform: 'uppercase',
    letterSpacing: '.02em',
  };

  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'flex-end' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <span style={legendaStyle}>Data</span>
        <input
          className={inputClassName}
          type="date"
          value={data}
          onChange={(e) => atualizar(e.target.value, h, m)}
          required={required}
        />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <span style={legendaStyle}>Hora</span>
        <select
          className={selectClassName}
          aria-label="Hora"
          value={h}
          onChange={(e) => atualizar(data, e.target.value, m)}
          required={required}
          style={{ width: 68 }}
        >
          <option value="" disabled>--</option>
          {HORAS.map((v) => <option key={v} value={v}>{v}</option>)}
        </select>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <span style={legendaStyle}>Min</span>
        <select
          className={selectClassName}
          aria-label="Minuto"
          value={m}
          onChange={(e) => atualizar(data, h, e.target.value)}
          required={required}
          style={{ width: 68 }}
        >
          <option value="" disabled>--</option>
          {MINUTOS.map((v) => <option key={v} value={v}>{v}</option>)}
        </select>
      </div>
    </div>
  );
}
