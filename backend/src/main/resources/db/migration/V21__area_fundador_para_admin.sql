-- Fase 5 — renomeia a área de RBAC "FUNDADOR" (E15) para "ADMIN" (pedido do cliente).
-- V1 já está aplicada em todo ambiente existente, então não dá pra editá-la — nova migration.

ALTER TABLE colaborador DROP CONSTRAINT chk_colaborador_area;

UPDATE colaborador SET area = 'ADMIN' WHERE area = 'FUNDADOR';

ALTER TABLE colaborador ADD CONSTRAINT chk_colaborador_area
    CHECK (area IN ('COMERCIAL', 'MARKETING', 'GESTAO_PERFORMANCE', 'ADMIN'));
