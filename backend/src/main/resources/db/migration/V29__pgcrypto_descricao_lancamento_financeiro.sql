-- Lacuna encontrada pelo revisor-seguranca ao confirmar a V28: ContaPagarReceberService.liquidar()
-- copia conta.getDescricao() (agora decriptado, ex. "Parcela 1 - Maria Souza") pra um
-- LancamentoFinanceiro novo — sem esta migration, o nome do lead/mentorado que a V28 acabou de
-- proteger reaparece em claro nesta terceira coluna. Nenhuma query filtra/ordena/faz LIKE em
-- `descricao` desta tabela (confirmado antes desta migration, mesmo critério de V19/V28).
ALTER TABLE lancamento_financeiro ALTER COLUMN descricao TYPE bytea USING pgp_sym_encrypt(descricao, '${pgcryptoKey}');
