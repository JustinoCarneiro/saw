CREATE TABLE conteudo_mentorado (
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id) ON DELETE CASCADE,
    conteudo_id     UUID NOT NULL REFERENCES conteudo(id) ON DELETE CASCADE,
    favorito        BOOLEAN NOT NULL DEFAULT false,
    assistido       BOOLEAN NOT NULL DEFAULT false,
    data_consumo    TIMESTAMP,
    PRIMARY KEY (mentorado_id, conteudo_id)
);

CREATE INDEX idx_conteudo_mentorado_favorito ON conteudo_mentorado(mentorado_id, favorito);
CREATE INDEX idx_conteudo_mentorado_assistido ON conteudo_mentorado(mentorado_id, assistido);
