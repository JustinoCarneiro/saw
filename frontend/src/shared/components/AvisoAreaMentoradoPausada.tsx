import { AREA_MENTORADO_PAUSADA } from '../lib/featureFlags';

// Reaproveitável em toda tela Admin onde a ação implica "isso fica visível/acessível pro
// mentorado" (publicar ata, conteúdo, aviso, evento) — a área de auto-atendimento do mentorado
// está pausada (AREA_MENTORADO_PAUSADA, decisão da reunião 17/07/2026), então nada disso chega
// no mentorado agora, mesmo que a ação em si funcione normalmente. Auto-contido: soma sozinho
// quando o flag voltar a `false`, sem precisar tocar em cada tela de novo.
export function AvisoAreaMentoradoPausada() {
  if (!AREA_MENTORADO_PAUSADA) return null;

  return (
    <div
      role="note"
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 10,
        padding: '10px 14px',
        borderRadius: 8,
        background: 'var(--warning-bg)',
        border: '1px solid var(--warning)',
        color: 'var(--warning)',
        fontSize: 13,
        lineHeight: 1.4,
        marginBottom: 16,
      }}
    >
      <span aria-hidden="true">⚠</span>
      <span>Área do mentorado pausada temporariamente — mentorados não conseguem ver isso agora.</span>
    </div>
  );
}
