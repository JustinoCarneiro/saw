// Stub local da Whisper API (OpenAI) + Messages API (Anthropic), só pro E2E (ver e2e-up.sh).
// Sem dependências além do Node — mesmo espírito do Mailpit pro SMTP: exercitar o caminho real
// de código (WhisperTranscricaoService/ClaudeAtaRascunhoService fazendo uma chamada HTTP de
// verdade e desserializando a resposta) sem custo nem chave real de API em CI/dev.
//
// Uso: node scripts/e2e-ia-stub-server.mjs [porta=8091]

import { createServer } from 'node:http';

const porta = Number(process.argv[2] ?? 8091);

export const TRANSCRICAO_STUB = 'Transcrição de teste E2E: o mentorado revisou o cardápio e discutiu '
  + 'precificação com o mentor. Ficou combinado atualizar a ficha técnica com os novos preços.';

export const RESUMO_STUB = 'Resumo gerado pela IA (stub E2E): revisão de cardápio e precificação, com '
  + 'encaminhamento de atualização da ficha técnica.';

// Change request 17/07/2026 ("campo Decisões na ata").
export const DECISOES_STUB = 'Decisão gerada pela IA (stub E2E): manter o cardápio atual até a próxima '
  + 'mentoria, só atualizando os preços da ficha técnica.';

function lerCorpo(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks)));
  });
}

const server = createServer(async (req, res) => {
  await lerCorpo(req); // drena o corpo (multipart do áudio ou JSON do Claude) sem precisar parsear

  if (req.method === 'POST' && req.url === '/v1/audio/transcriptions') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ text: TRANSCRICAO_STUB }));
    return;
  }

  if (req.method === 'POST' && req.url === '/v1/messages') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      content: [
        {
          type: 'tool_use',
          name: 'registrar_rascunho_ata',
          input: { resumo: RESUMO_STUB, decisoes: DECISOES_STUB },
        },
      ],
    }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: `stub não implementa ${req.method} ${req.url}` }));
});

server.listen(porta, () => {
  console.log(`Stub IA (Whisper + Claude) escutando em http://localhost:${porta}`);
});
