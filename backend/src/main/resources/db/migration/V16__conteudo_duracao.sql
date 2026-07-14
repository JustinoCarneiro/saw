-- H6.3 — "minutos assistidos" nos indicadores de consumo do mentorado. Duração DECLARADA do
-- material (Admin preenche ao cadastrar), não tempo real de exibição — o projeto não tem
-- rastreamento de player, então isto é o que dá pra medir sem inventar um número.
ALTER TABLE conteudo ADD COLUMN duracao_minutos INTEGER;
