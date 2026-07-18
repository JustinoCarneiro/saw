export function PausedScreen({ title, description }: { title: string; description: string }) {
  return (
    <div
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 60,
        gap: 16,
        minHeight: '60vh',
      }}
    >
      <div style={{ fontSize: 20, fontWeight: 700 }}>{title}</div>
      <div style={{ fontSize: 14, color: 'var(--text-soft)', maxWidth: 420 }}>{description}</div>
    </div>
  );
}
