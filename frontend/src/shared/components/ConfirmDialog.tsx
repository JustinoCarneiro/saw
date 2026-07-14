import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  submitting?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

// Ações críticas (cancelar mentoria/evento/pedido, reembolsar, desativar mentorado, publicar
// ata) disparavam a mutação direto no onClick, sem chance de desistir de um clique errado —
// pedido explícito do cliente. Overlay simples porque não existe nenhum padrão de modal no
// sistema ainda pra seguir (nem no protótipo congelado, nem no DESIGN.md) — usa só os tokens já
// documentados (superfície, sombra, raio), não inventa paleta nova.
export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  danger = true,
  submitting = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <div className={styles.overlay} onClick={submitting ? undefined : onCancel}>
      <div
        className={styles.dialog}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.title} id="confirm-dialog-title">
          {title}
        </div>
        <div className={styles.message}>{message}</div>
        <div className={styles.actions}>
          <button type="button" className={styles.cancelButton} onClick={onCancel} disabled={submitting}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? styles.dangerButton : styles.confirmButton}
            onClick={onConfirm}
            disabled={submitting}
          >
            {submitting ? 'Processando…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
