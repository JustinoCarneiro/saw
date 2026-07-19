-- Change request 17/07/2026 ("campo Decisões na ata") — a ata real da operação
-- (PDF "ATA DE REUNIÃO 07/06/2026") tem Participantes/Pauta/Encaminhamentos, faltava uma seção
-- própria de Decisões, distinta do resumo livre. Mesmo tratamento de "resumo" (text simples, sem
-- pgcrypto, mesmo critério já usado na tabela ata).
ALTER TABLE ata ADD COLUMN decisoes TEXT;
