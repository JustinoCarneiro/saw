-- H15.6/H15.7 (M20): carteira e conversao_pct nunca eram calculadas, só o valor literal que o
-- seeder escreveu na primeira carga. carteira passa a ser sempre computada a partir de
-- Mentoria.mentor; conversao_pct é substituída pelo endpoint de desempenho por período.
ALTER TABLE colaborador DROP COLUMN carteira;
ALTER TABLE colaborador DROP COLUMN conversao_pct;
