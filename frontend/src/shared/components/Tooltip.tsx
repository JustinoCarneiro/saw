import { type ReactNode, useState } from 'react';
import styles from './Tooltip.module.css';

// Achado de UX (22/07/2026, pedido do Marcos): títulos de seção/subseção usam termos do negócio
// (ex. "Risco de churn", "Frequência em mentoria") que não são autoexplicativos — passar o mouse
// mostra um resumo de uma frase do que aquilo significa/de onde vem. Delay pequeno no mouseEnter
// evita o "flicker" de abrir/fechar ao só passar o cursor de raspão por cima.
export function Tooltip({ text, children }: { text: string; children: ReactNode }) {
  const [visivel, setVisivel] = useState(false);
  const [timer, setTimer] = useState<number | undefined>(undefined);

  function agendarAbertura() {
    setTimer(window.setTimeout(() => setVisivel(true), 250));
  }

  function fechar() {
    window.clearTimeout(timer);
    setVisivel(false);
  }

  return (
    <span
      className={styles.trigger}
      tabIndex={0}
      onMouseEnter={agendarAbertura}
      onMouseLeave={fechar}
      onFocus={agendarAbertura}
      onBlur={fechar}
    >
      {children}
      {visivel && (
        <span className={styles.bubble} role="tooltip">
          {text}
        </span>
      )}
    </span>
  );
}
