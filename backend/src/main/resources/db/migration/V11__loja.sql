-- M14: E8 · Loja SAW — catálogo, carrinho, checkout, gateway (Mercado Pago).
CREATE TABLE produto (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo            VARCHAR(255) NOT NULL,
    descricao         TEXT NOT NULL,
    categoria         VARCHAR(20) NOT NULL,
    preco             NUMERIC(10,2) NOT NULL,
    preco_original    NUMERIC(10,2),
    avaliacao_media   NUMERIC(2,1),
    destaque          BOOLEAN NOT NULL DEFAULT false,
    vendas            INT NOT NULL DEFAULT 0,
    arquivo_url       VARCHAR(500) NOT NULL,
    imagem_url        VARCHAR(500),
    publicado         BOOLEAN NOT NULL DEFAULT false,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_produto_categoria CHECK (categoria IN
        ('CURSO','PLANILHA','TEMPLATE','EBOOK','FERRAMENTA','KIT','CONSULTORIA')),
    CONSTRAINT chk_produto_preco CHECK (preco > 0)
);
CREATE INDEX idx_produto_categoria ON produto(categoria);
CREATE INDEX idx_produto_publicado ON produto(publicado);

CREATE TABLE pedido (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id          UUID NOT NULL REFERENCES mentorado(id),
    status                VARCHAR(25) NOT NULL DEFAULT 'CARRINHO',
    valor_total           NUMERIC(10,2) NOT NULL DEFAULT 0,
    referencia_gateway    VARCHAR(255),
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pedido_status CHECK (status IN
        ('CARRINHO','AGUARDANDO_PAGAMENTO','PAGO','LIBERADO','CANCELADO','REEMBOLSADO'))
);
-- Só 1 carrinho ATIVO por mentorado — índice parcial, não constraint de tabela inteira (o
-- mentorado pode ter vários pedidos PAGO/LIBERADO no histórico, só não 2 CARRINHO ao mesmo tempo).
CREATE UNIQUE INDEX idx_pedido_carrinho_unico ON pedido(mentorado_id) WHERE status = 'CARRINHO';
CREATE INDEX idx_pedido_mentorado ON pedido(mentorado_id);

CREATE TABLE item_pedido (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id         UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id        UUID NOT NULL REFERENCES produto(id),
    quantidade        INT NOT NULL,
    preco_unitario    NUMERIC(10,2) NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_item_pedido_quantidade CHECK (quantidade > 0)
);
CREATE UNIQUE INDEX idx_item_pedido_unico ON item_pedido(pedido_id, produto_id);
