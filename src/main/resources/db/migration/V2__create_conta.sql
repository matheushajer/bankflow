CREATE TABLE conta (
    id                      UNIQUEIDENTIFIER NOT NULL,
    cliente_id              UNIQUEIDENTIFIER NOT NULL,
    numero                  VARCHAR(20)       NOT NULL,
    agencia                 VARCHAR(10)       NOT NULL,
    tipo                    VARCHAR(20)       NOT NULL,
    saldo                   DECIMAL(18,2)     NOT NULL,
    limite_cheque_especial  DECIMAL(18,2)     NOT NULL,
    status                  VARCHAR(20)       NOT NULL,
    data_abertura           DATETIME2         NOT NULL,
    version                 BIGINT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_conta PRIMARY KEY (id),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE UNIQUE INDEX ix_conta_numero ON conta (numero);
CREATE INDEX ix_conta_cliente_id ON conta (cliente_id);
