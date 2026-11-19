-- ON DELETE CASCADE: excluir um evento em RASCUNHO leva junto categorias, itens e alérgenos
CREATE TABLE categoria (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id BIGINT      NOT NULL REFERENCES evento (id) ON DELETE CASCADE,
    nome      VARCHAR(80) NOT NULL,
    meta      INTEGER     NOT NULL,
    unidade   VARCHAR(20) NOT NULL,

    CONSTRAINT ck_categoria_meta CHECK (meta > 0),
    CONSTRAINT ck_categoria_unidade CHECK (unidade IN ('UNIDADE', 'LITRO', 'KG', 'PACOTE', 'GARRAFA'))
);

CREATE INDEX idx_categoria_evento ON categoria (evento_id);

CREATE TABLE item (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    categoria_id          BIGINT       NOT NULL REFERENCES categoria (id) ON DELETE CASCADE,
    nome                  VARCHAR(120) NOT NULL,
    quantidade_necessaria INTEGER      NOT NULL,
    obrigatorio           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT ck_item_quantidade CHECK (quantidade_necessaria > 0)
);

CREATE INDEX idx_item_categoria ON item (categoria_id);

CREATE TABLE item_alergeno (
    item_id  BIGINT      NOT NULL REFERENCES item (id) ON DELETE CASCADE,
    alergeno VARCHAR(20) NOT NULL,

    PRIMARY KEY (item_id, alergeno),
    CONSTRAINT ck_item_alergeno CHECK (alergeno IN (
        'GLUTEN', 'LACTOSE', 'OVO', 'CARNE', 'PEIXE', 'FRUTOS_DO_MAR', 'MEL', 'AMENDOIM'))
);
