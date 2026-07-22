const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function formatBRL(value: number): string {
  return BRL.format(value);
}

export function formatPct(value: number, digits = 1): string {
  const sign = value > 0 ? '+' : '';
  return `${sign}${value.toFixed(digits)}%`;
}

export const MESES_PT = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

// "há 5 min" / "há 3h" / "há 2d" — usado por AvisosPage e pelo widget "Avisos importantes" do
// Dashboard do mentorado.
export function formatarQuando(iso: string): string {
  const diffMin = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (diffMin < 60) return `há ${diffMin} min`;
  const diffH = Math.round(diffMin / 60);
  if (diffH < 24) return `há ${diffH}h`;
  return `há ${Math.round(diffH / 24)}d`;
}

// Auditoria de UX (22/07/2026) — máscara de digitação pro CNPJ (00.000.000/0000-00), usada em
// onChange dos inputs de CNPJ. Puramente de exibição: o valor sem máscara chega igual pro
// backend (a validação de formato real é o @Pattern de AtualizarDadosContratoRequest).
export function formatarCnpj(value: string): string {
  const digitos = value.replace(/\D/g, '').slice(0, 14);
  const partes = [digitos.slice(0, 2), digitos.slice(2, 5), digitos.slice(5, 8), digitos.slice(8, 12), digitos.slice(12, 14)];
  let resultado = partes[0];
  if (partes[1]) resultado += `.${partes[1]}`;
  if (partes[2]) resultado += `.${partes[2]}`;
  if (partes[3]) resultado += `/${partes[3]}`;
  if (partes[4]) resultado += `-${partes[4]}`;
  return resultado;
}

// Auditoria de UX (22/07/2026) — máscara de telefone, celular (11 dígitos, DDD+9) ou fixo (10
// dígitos, DDD+8). Desvia pro formato de celular assim que o 9º dígito depois do DDD é digitado,
// mesmo comportamento das máscaras de telefone usuais em pt-BR.
export function formatarTelefone(value: string): string {
  const digitos = value.replace(/\D/g, '').slice(0, 11);
  if (digitos.length === 0) return '';
  const ddd = digitos.slice(0, 2);
  const restante = digitos.slice(2);
  let resultado = `(${ddd}`;
  if (ddd.length === 2) resultado += ')';
  if (restante.length === 0) return resultado;
  const tamanhoPrefixo = restante.length > 8 ? 5 : 4;
  const prefixo = restante.slice(0, tamanhoPrefixo);
  const sufixo = restante.slice(tamanhoPrefixo);
  resultado += ` ${prefixo}`;
  if (sufixo) resultado += `-${sufixo}`;
  return resultado;
}
