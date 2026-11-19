CREATE TABLE evento (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome             VARCHAR(120)  NOT NULL,
    descricao        VARCHAR(1000),
    tipo             VARCHAR(30)   NOT NULL,
    data_hora        TIMESTAMP     NOT NULL,
    prazo_alteracoes TIMESTAMP     NOT NULL,
    status           VARCHAR(20)   NOT NULL,

    CONSTRAINT ck_evento_tipo
        CHECK (tipo IN ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'CONFRATERNIZACAO', 'OUTRO')),
    CONSTRAINT ck_evento_status
        CHECK (status IN ('RASCUNHO', 'ABERTO', 'ENCERRADO', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT ck_evento_prazo_antes_da_data
        CHECK (prazo_alteracoes < data_hora)
);

CREATE INDEX idx_evento_status ON evento (status);
