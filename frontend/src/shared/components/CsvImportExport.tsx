import { type ChangeEvent, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { apiClient } from '../lib/apiClient';
import { getApiErrorMessage } from '../lib/apiError';
import type { ImportResultResponse } from '../lib/types';
import styles from './CsvImportExport.module.css';

interface CsvImportExportProps {
  exportUrl: string;
  exportParams?: Record<string, string | undefined>;
  exportFilename: string;
  importUrl: string;
  onImportado: () => void;
}

// M21 — "Importar" é tudo-ou-nada (ver Blueprint, ROADMAP.md): o backend só devolve 200 quando
// TODAS as linhas foram validadas e persistidas; 422 significa que nada foi salvo, e o corpo traz
// o relatório de erro por linha pro usuário corrigir o arquivo e reenviar.
export function CsvImportExport({ exportUrl, exportParams, exportFilename, importUrl, onImportado }: CsvImportExportProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [exportando, setExportando] = useState(false);
  const [importando, setImportando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [resultado, setResultado] = useState<ImportResultResponse | null>(null);

  async function exportar() {
    setExportando(true);
    setErro(null);
    try {
      const res = await apiClient.get(exportUrl, { params: exportParams, responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = exportFilename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível exportar o CSV.'));
    } finally {
      setExportando(false);
    }
  }

  async function importar(e: ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    if (!arquivo) return;
    setImportando(true);
    setErro(null);
    setResultado(null);
    const form = new FormData();
    form.append('arquivo', arquivo);
    try {
      const res = await apiClient.post<ImportResultResponse>(importUrl, form);
      setResultado(res.data);
      if (res.data.erros.length === 0) {
        onImportado();
      }
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 422 && Array.isArray(err.response.data?.erros)) {
        setResultado(err.response.data as ImportResultResponse);
      } else {
        setErro(getApiErrorMessage(err, 'Não foi possível importar o arquivo.'));
      }
    } finally {
      setImportando(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.buttons}>
        <button type="button" className={styles.button} onClick={exportar} disabled={exportando} data-testid="csv-exportar">
          {exportando ? 'Exportando…' : 'Exportar CSV'}
        </button>
        <button
          type="button"
          className={styles.button}
          onClick={() => fileInputRef.current?.click()}
          disabled={importando}
          data-testid="csv-importar-botao"
        >
          {importando ? 'Importando…' : 'Importar CSV'}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          hidden
          onChange={importar}
          data-testid="csv-importar-input"
        />
      </div>

      {erro && <div className={styles.erro}>{erro}</div>}
      {resultado && resultado.erros.length === 0 && (
        <div className={styles.sucesso} data-testid="csv-import-sucesso">
          {resultado.importados} linha(s) importada(s) com sucesso.
        </div>
      )}
      {resultado && resultado.erros.length > 0 && (
        <div className={styles.erro} data-testid="csv-import-erros">
          <div>Nenhuma linha foi importada — corrija o arquivo e reenvie:</div>
          <ul>
            {resultado.erros.map((e) => (
              <li key={e.linha}>Linha {e.linha}: {e.motivo}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
