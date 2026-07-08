import { isAxiosError } from 'axios';

/** O backend sempre devolve { message: string } em erro (ApiError) — mostra essa mensagem real
 * em vez de um chute genérico, que pode estar simplesmente errado sobre a causa (ex.: mostrar
 * "já foi liquidada" quando na verdade foi "conta sem categoria"). */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err) && typeof err.response?.data?.message === 'string') {
    return err.response.data.message;
  }
  return fallback;
}
