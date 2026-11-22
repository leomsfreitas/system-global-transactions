-- V3: indices da especificacao e unicidades para idempotencia dos jobs (INSERT ... ON CONFLICT DO NOTHING).

-- cotacao: o UNIQUE (moeda_base_id, moeda_destino_id, data_hora) da V1 ja cria o indice composto
-- pedido na especificacao, propagado para cada particao. Indice auxiliar para buscas por fonte:
CREATE INDEX ix_cotacao_fonte_data ON cotacao (fonte_id, data_hora DESC);

-- indicador_economico: idempotencia por (pais, tipo, periodo, fonte) e busca do mais recente por tipo.
CREATE UNIQUE INDEX uq_indicador_pais_tipo_periodo_fonte
    ON indicador_economico (pais_id, tipo, periodo, fonte_id);
CREATE INDEX ix_indicador_pais_tipo_periodo ON indicador_economico (pais_id, tipo, periodo DESC);

-- indice_geopolitico: indice da especificacao e idempotencia.
CREATE INDEX ix_indice_pais_data ON indice_geopolitico (pais_id, data_referencia DESC);
CREATE UNIQUE INDEX uq_indice_pais_data_fonte
    ON indice_geopolitico (pais_id, data_referencia, fonte_id);

-- conversao: consultas de auditoria por periodo.
CREATE INDEX ix_conversao_data_hora ON conversao (data_hora);

-- moeda: lookup por pais (estrategias de ajuste).
CREATE INDEX ix_moeda_pais ON moeda (pais_id);
