/**
 * "Raio-X" das planilhas — mapeia a ESTRUTURA (não só o dado): valor + fórmula de cada célula,
 * células mescladas (cabeçalhos agrupados tipo "Total Despesas_2"), intervalos nomeados,
 * regras de validação/dropdown (ex. "Status do Pagamento", "Tipo de ingresso" — pega os valores
 * permitidos direto da fonte, sem depender de suposição), e sinaliza toda fórmula que referencia
 * outra aba ou outra planilha (INDIRECT/IMPORTRANGE/`Aba!Célula`) — é isso que revela como os
 * totais dependem dos detalhes ("o sistema nervoso" entre as abas).
 *
 * SOMENTE LEITURA, GARANTIDO: todo método Apps Script usado abaixo é um *getter*
 * (getValues, getFormulas, getDataValidations, getMergedRanges, getNamedRanges,
 * getFrozenRows/Columns, getSheets, getUrl, getName) — nenhum deles grava nada na planilha de
 * origem. O único efeito colateral de rodar este script é criar um ARQUIVO NOVO no seu Google
 * Drive com o relatório; a estrutura/dado das planilhas fonte não é tocada em nenhum momento.
 *
 * Ver docs/reuniao-2026-07-17-atualizacoes.md § "Como passar a estrutura de planilhas/Notion
 * recebida". Usado pra entender a modelagem real (M24/M25) antes de desenhar o import.
 *
 * COMO USAR:
 * 1. Abra script.google.com (ou, dentro de qualquer planilha: Extensões → Apps Script).
 * 2. Cole este código inteiro (substitua o conteúdo padrão).
 * 3. Em PLANILHAS_PARA_EXPORTAR, cole a URL (ou só o ID) de cada planilha que você quer
 *    mapear — pode ser mais de uma.
 * 4. No dropdown de função (topo do editor), selecione "raioXTudo" e clique em ▶ Executar.
 * 5. Na primeira execução o Google vai pedir autorização (normal pra script pessoal seu,
 *    mesmo se aparecer aviso de "app não verificado" — é você autorizando seu próprio script).
 * 6. Se a planilha for grande, pode demorar — acompanhe em "Execuções"/"Ver → Registros".
 *    Se estourar o limite de tempo do Apps Script (6 min em conta pessoal), rode uma planilha
 *    de cada vez (deixe só uma URL no array por execução).
 * 7. No fim, o log mostra o link do arquivo .json criado no seu Drive. Baixe e solte em
 *    dados-cliente-notion/ na raiz do projeto (já existe, já está no .gitignore).
 */

const PLANILHAS_PARA_EXPORTAR = [
  'COLE_AQUI_A_URL_OU_ID_DA_PLANILHA_1',
  // 'COLE_AQUI_A_URL_OU_ID_DA_PLANILHA_2',
];

function raioXTudo() {
  const resultado = {
    geradoEm: new Date().toISOString(),
    planilhas: [],
  };

  PLANILHAS_PARA_EXPORTAR.forEach(function (referencia) {
    const id = extrairId(referencia);
    const planilha = SpreadsheetApp.openById(id); // leitura — abrir não altera nada

    Logger.log('Lendo planilha: ' + planilha.getName());

    const infoPlanilha = {
      nome: planilha.getName(),
      id: id,
      url: planilha.getUrl(),
      intervalosNomeados: planilha.getNamedRanges().map(function (nr) {
        return {
          nome: nr.getName(),
          aba: nr.getRange().getSheet().getName(),
          intervalo: nr.getRange().getA1Notation(),
        };
      }),
      abas: [],
    };

    planilha.getSheets().forEach(function (aba) {
      Logger.log('  aba: ' + aba.getName());

      const intervalo = aba.getDataRange();
      const valores = intervalo.getValues();
      const formulas = intervalo.getFormulas();
      const validacoes = intervalo.getDataValidations();

      const celulas = [];
      const referenciasCruzadas = []; // fórmulas que apontam pra outra aba ou outra planilha

      for (let linha = 0; linha < valores.length; linha++) {
        for (let coluna = 0; coluna < valores[linha].length; coluna++) {
          const valor = valores[linha][coluna];
          const formula = formulas[linha][coluna];
          const regraValidacao = validacoes[linha][coluna];

          if (valor === '' && formula === '' && !regraValidacao) continue;

          const enderecoCelula = aba.getRange(linha + 1, coluna + 1).getA1Notation();
          const item = { celula: enderecoCelula, valor: valor, formula: formula || null };

          if (regraValidacao) {
            item.validacao = descreverValidacao(regraValidacao);
          }

          celulas.push(item);

          // '!' = referência tipo `NomeDaAba!A1`; IMPORTRANGE = puxa de outra planilha inteira.
          if (formula && (formula.indexOf('!') !== -1 || formula.indexOf('IMPORTRANGE') !== -1)) {
            referenciasCruzadas.push({ celula: enderecoCelula, formula: formula });
          }
        }
      }

      infoPlanilha.abas.push({
        nome: aba.getName(),
        linhasCongeladas: aba.getFrozenRows(),
        colunasCongeladas: aba.getFrozenColumns(),
        celulasMescladas: intervalo.getMergedRanges().map(function (r) {
          return r.getA1Notation();
        }),
        referenciasCruzadas: referenciasCruzadas,
        celulas: celulas,
      });
    });

    resultado.planilhas.push(infoPlanilha);
  });

  const nomeArquivo = 'raio-x-planilhas-' +
      Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyy-MM-dd-HHmm') + '.json';
  const arquivo = DriveApp.createFile(nomeArquivo, JSON.stringify(resultado, null, 2), MimeType.PLAIN_TEXT);

  Logger.log('Raio-X concluído.');
  Logger.log('Arquivo criado no seu Drive: ' + arquivo.getUrl());
}

// Descreve uma regra de validação de dado (dropdown) de forma serializável em JSON — o objeto
// DataValidation do Apps Script não vira JSON direto (pode conter um Range como critério).
function descreverValidacao(regra) {
  const tipo = String(regra.getCriteriaType());
  let valores;
  try {
    valores = regra.getCriteriaValues().map(function (v) {
      // Um dos critérios pode ser um Range (ex. "lista vem do intervalo X") — Range não
      // serializa em JSON.stringify, precisa virar A1 notation manualmente.
      if (v && typeof v.getA1Notation === 'function') {
        return 'intervalo: ' + v.getA1Notation();
      }
      return v;
    });
  } catch (e) {
    valores = 'não foi possível ler os critérios (' + e.message + ')';
  }
  return { tipo: tipo, criterios: valores };
}

function extrairId(referencia) {
  const match = referencia.match(/\/d\/([a-zA-Z0-9-_]+)/);
  return match ? match[1] : referencia; // se não bater o padrão de URL, assume que já é o ID puro
}
