-- V1: schema base com as 7 tabelas do modelo de dados (AT05).
-- Decisoes: NUMERIC para dinheiro; TIMESTAMPTZ em vez de TIMESTAMP (ADR 0004);
-- historico nunca sobrescrito; cotacao particionada por mes (ADR 0012, particoes em V2).

CREATE TABLE pais (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_iso  CHAR(2)      NOT NULL UNIQUE CHECK (codigo_iso ~ '^[A-Z]{2}$'),
    -- ISO 3166 alfa-3, usado pelo FMI. Nulo para agregados como EU (ADR 0008).
    codigo_iso3 CHAR(3)      UNIQUE CHECK (codigo_iso3 ~ '^[A-Z]{3}$'),
    nome        VARCHAR(100) NOT NULL,
    regiao      VARCHAR(50)  NOT NULL
);

CREATE TABLE moeda (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_iso     CHAR(3)      NOT NULL UNIQUE CHECK (codigo_iso ~ '^[A-Z]{3}$'),
    nome           VARCHAR(100) NOT NULL,
    simbolo        VARCHAR(10)  NOT NULL,
    -- Casas decimais ISO 4217 (JPY = 0). Usado no arredondamento do valor convertido (ADR 0010).
    casas_decimais SMALLINT     NOT NULL DEFAULT 2 CHECK (casas_decimais BETWEEN 0 AND 4),
    pais_id        BIGINT       NOT NULL REFERENCES pais (id)
);

CREATE TABLE fonte_dados (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Codigo estavel usado pelo codigo (BCB, BCE, FMI, BANCO_MUNDIAL)
    codigo   VARCHAR(30)  NOT NULL UNIQUE,
    nome     VARCHAR(100) NOT NULL,
    url_base VARCHAR(255) NOT NULL,
    tipo     VARCHAR(30)  NOT NULL CHECK (tipo IN ('COTACAO', 'INDICADOR', 'COTACAO_E_INDICADOR')),
    ativa    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Particionada por mes. Em tabela particionada a PK precisa incluir a coluna de particao (ADR 0012).
CREATE TABLE cotacao (
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    moeda_base_id    BIGINT         NOT NULL REFERENCES moeda (id),
    moeda_destino_id BIGINT         NOT NULL REFERENCES moeda (id),
    valor            NUMERIC(18, 6) NOT NULL CHECK (valor > 0),
    data_hora        TIMESTAMPTZ    NOT NULL,
    fonte_id         BIGINT         NOT NULL REFERENCES fonte_dados (id),
    CONSTRAINT pk_cotacao PRIMARY KEY (id, data_hora),
    CONSTRAINT uq_cotacao_par_data UNIQUE (moeda_base_id, moeda_destino_id, data_hora),
    CONSTRAINT ck_cotacao_par_distinto CHECK (moeda_base_id <> moeda_destino_id)
) PARTITION BY RANGE (data_hora);

CREATE TABLE indicador_economico (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id  BIGINT         NOT NULL REFERENCES pais (id),
    -- ESTABILIDADE_POLITICA guarda o PV.EST bruto usado no indice (ADR 0005); a API so expoe os 3 primeiros.
    tipo     VARCHAR(30)    NOT NULL CHECK (tipo IN ('INFLACAO', 'JUROS', 'PIB', 'ESTABILIDADE_POLITICA')),
    valor    NUMERIC(24, 6) NOT NULL,
    periodo  DATE           NOT NULL,
    fonte_id BIGINT         NOT NULL REFERENCES fonte_dados (id)
);

CREATE TABLE indice_geopolitico (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id         BIGINT        NOT NULL REFERENCES pais (id),
    indice_risco    NUMERIC(5, 2) NOT NULL CHECK (indice_risco BETWEEN 0 AND 100),
    fator_ajuste    NUMERIC(6, 4) NOT NULL CHECK (fator_ajuste > 0 AND fator_ajuste <= 1),
    data_referencia DATE          NOT NULL,
    fonte_id        BIGINT        NOT NULL REFERENCES fonte_dados (id)
);

-- Auditoria de cada conversao bem-sucedida (ADR 0014: fonte_id e api_key_hash_prefixo).
CREATE TABLE conversao (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    moeda_origem_id      BIGINT         NOT NULL REFERENCES moeda (id),
    moeda_destino_id     BIGINT         NOT NULL REFERENCES moeda (id),
    valor_original       NUMERIC(20, 6) NOT NULL CHECK (valor_original > 0),
    taxa_aplicada        NUMERIC(18, 6) NOT NULL CHECK (taxa_aplicada > 0),
    fator_geopolitico    NUMERIC(6, 4)  NOT NULL CHECK (fator_geopolitico > 0 AND fator_geopolitico <= 1),
    valor_convertido     NUMERIC(24, 6) NOT NULL CHECK (valor_convertido >= 0),
    data_hora            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fonte_id             BIGINT         REFERENCES fonte_dados (id),
    api_key_hash_prefixo CHAR(8)
);
