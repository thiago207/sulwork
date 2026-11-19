CREATE TABLE participante (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                 VARCHAR(120) NOT NULL,
    cpf                  VARCHAR(11)  NOT NULL,
    observacao_restricao VARCHAR(500),
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_participante_cpf UNIQUE (cpf),
    CONSTRAINT ck_participante_cpf CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE TABLE participante_restricao (
    participante_id BIGINT      NOT NULL REFERENCES participante (id),
    restricao       VARCHAR(30) NOT NULL,

    PRIMARY KEY (participante_id, restricao),
    CONSTRAINT ck_participante_restricao CHECK (restricao IN (
        'VEGANO', 'VEGETARIANO', 'SEM_GLUTEN', 'SEM_LACTOSE',
        'ALERGIA_AMENDOIM', 'ALERGIA_FRUTOS_DO_MAR', 'ALERGIA_OVO'))
);
