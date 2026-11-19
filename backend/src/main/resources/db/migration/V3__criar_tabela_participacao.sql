CREATE TABLE participacao (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id       BIGINT  NOT NULL REFERENCES evento (id),
    participante_id BIGINT  NOT NULL REFERENCES participante (id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,

    -- vale também para linhas inativas: voltar ao evento reativa a linha antiga
    CONSTRAINT uk_participacao_evento_participante UNIQUE (evento_id, participante_id)
);

CREATE INDEX idx_participacao_participante ON participacao (participante_id);
