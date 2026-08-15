CREATE TABLE cliente (
    id               UNIQUEIDENTIFIER NOT NULL,
    nome             NVARCHAR(200)    NOT NULL,
    cpf              VARCHAR(11)      NOT NULL,
    data_nascimento  DATE             NOT NULL,
    email            VARCHAR(150)     NOT NULL,
    status           VARCHAR(20)      NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_email UNIQUE (email)
);

-- indice unico: garante unicidade de cpf e acelera consultas por cpf
CREATE UNIQUE INDEX ix_cliente_cpf ON cliente (cpf);
