-- Change request pós-MVP (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md) —
-- confirmado pelo cliente que o campo não se aplica ("é geral, não precisa dessa área": as
-- mentorias/consultorias não são segmentadas por área de interesse do mentorado).
ALTER TABLE mentorado DROP COLUMN areas_interesse;
