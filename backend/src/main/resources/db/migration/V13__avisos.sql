CREATE TABLE aviso (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo       VARCHAR(200) NOT NULL,
    descricao    VARCHAR(1000) NOT NULL,
    categoria    VARCHAR(20) NOT NULL,
    plano_minimo VARCHAR(20) NOT NULL DEFAULT 'GRATUITO',
    criado_em    TIMESTAMP NOT NULL DEFAULT now(),
    versao       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_aviso_categoria CHECK (categoria IN ('GERAL','MENTORIAS','MATERIAIS','EVENTOS'))
);

CREATE TABLE aviso_mentorado (
    mentorado_id UUID NOT NULL REFERENCES mentorado(id),
    aviso_id     UUID NOT NULL REFERENCES aviso(id),
    lido         BOOLEAN NOT NULL DEFAULT false,
    lido_em      TIMESTAMP,
    versao       BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (mentorado_id, aviso_id)
);
