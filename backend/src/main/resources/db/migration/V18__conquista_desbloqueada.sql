-- H9.2 — data de desbloqueio de conquista + histórico de XP (ver ROADMAP.md, pendência
-- documentada desde o M15: "XP/nível/conquistas calculados por leitura, sem persistência").
--
-- conquistas_observadas_em marca a PRIMEIRA VEZ que a jornada de um mentorado foi computada
-- depois desta migração — distingue "essa conquista já era verdadeira antes de rastrearmos"
-- (desbloqueada_em NULL, mostrado como "Desde sempre") de "essa conquista acabou de acontecer"
-- (desbloqueada_em com data real). Sem essa marca, a primeira leitura pós-deploy de QUALQUER
-- mentorado gravaria hoje como data de desbloqueio de conquistas que na verdade já eram
-- verdadeiras há meses — uma data fabricada, não uma data real.
ALTER TABLE mentorado ADD COLUMN conquistas_observadas_em TIMESTAMP;

CREATE TABLE conquista_desbloqueada (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id),
    codigo          VARCHAR(40) NOT NULL,
    desbloqueada_em TIMESTAMP,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_conquista_desbloqueada_mentorado_codigo UNIQUE (mentorado_id, codigo)
);
