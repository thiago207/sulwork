CREATE TABLE contribuicao (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participacao_id BIGINT  NOT NULL REFERENCES participacao (id),
    item_id         BIGINT  NOT NULL REFERENCES item (id) ON DELETE CASCADE,
    quantidade      INTEGER NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT ck_contribuicao_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_contribuicao_item ON contribuicao (item_id);

-- no máximo uma contribuição ativa por participante e item; as inativas não contam
CREATE UNIQUE INDEX uk_contribuicao_ativa ON contribuicao (participacao_id, item_id) WHERE ativo;
