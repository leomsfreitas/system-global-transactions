-- V2: particoes mensais de cotacao (ADR 0012).
-- 24 meses anteriores + mes corrente + 3 seguintes, relativos a data em que a migration roda,
-- e uma particao DEFAULT para qualquer dado fora dessa janela.

-- Cria (se nao existir) a particao do mes que contem p_mes. Idempotente.
-- SECURITY DEFINER: roda com o dono da tabela, porque criar particao exige ser dono da tabela-mae.
-- O worker recebe so EXECUTE nesta funcao (V5), sem privilegio de DDL.
CREATE OR REPLACE FUNCTION criar_particao_cotacao(p_mes DATE)
    RETURNS TEXT
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
    SET timezone = 'UTC'
AS
$$
DECLARE
    v_inicio DATE := date_trunc('month', p_mes)::DATE;
    v_fim    DATE := (date_trunc('month', p_mes) + INTERVAL '1 month')::DATE;
    v_nome   TEXT := format('cotacao_%s', to_char(v_inicio, 'YYYY_MM'));
BEGIN
    IF to_regclass(v_nome) IS NOT NULL THEN
        RETURN v_nome;
    END IF;
    EXECUTE format(
            'CREATE TABLE %I PARTITION OF cotacao FOR VALUES FROM (%L) TO (%L)',
            v_nome, v_inicio::TIMESTAMPTZ, v_fim::TIMESTAMPTZ);
    RETURN v_nome;
END;
$$;

-- As fronteiras usam o fuso da sessao; fixamos UTC para que a particao de um mes seja sempre
-- [YYYY-MM-01 00:00 UTC, mes seguinte 00:00 UTC).
SET TIME ZONE 'UTC';

DO
$$
    DECLARE
        v_mes DATE;
    BEGIN
        FOR v_mes IN
            SELECT generate_series(
                           date_trunc('month', now()) - INTERVAL '24 months',
                           date_trunc('month', now()) + INTERVAL '3 months',
                           INTERVAL '1 month')::DATE
            LOOP
                PERFORM criar_particao_cotacao(v_mes);
            END LOOP;
    END;
$$;

CREATE TABLE cotacao_default PARTITION OF cotacao DEFAULT;
