-- Seed do ambiente local (NAO e migration): 90 dias uteis de cotacoes, indices e indicadores de exemplo,
-- para os 6 endpoints responderem logo apos o make up. Idempotente (ON CONFLICT DO NOTHING).
-- O ultimo dia util traz o exemplo canonico: USD->BRL 5,4210 e indice do BR 30,00 (fator 0,9850).
-- Valores-base proximos dos reais de nov/2026 (fixtures da AT01), com oscilacao senoidal de ate 2%.

WITH dias AS (
    SELECT d::date AS dia, row_number() OVER (ORDER BY d DESC) - 1 AS n
    FROM generate_series(current_date - 140, current_date, INTERVAL '1 day') d
    WHERE extract(isodow FROM d) < 6
),
base(base, destino, valor, fonte, hora) AS (
    VALUES ('USD', 'BRL', 5.4210, 'BCB', TIME '16:00'),
           ('EUR', 'BRL', 6.2950, 'BCB', TIME '16:00'),
           ('GBP', 'BRL', 7.1500, 'BCB', TIME '16:00'),
           ('JPY', 'BRL', 0.035900, 'BCB', TIME '16:00'),
           ('CHF', 'BRL', 6.7800, 'BCB', TIME '16:00'),
           ('CAD', 'BRL', 3.9200, 'BCB', TIME '16:00'),
           ('AUD', 'BRL', 3.5400, 'BCB', TIME '16:00'),
           ('EUR', 'USD', 1.1612, 'BCE', TIME '13:15'),
           ('EUR', 'BRL', 6.2950, 'BCE', TIME '13:15'),
           ('EUR', 'GBP', 0.8800, 'BCE', TIME '13:15'),
           ('EUR', 'JPY', 175.30, 'BCE', TIME '13:15'),
           ('EUR', 'CHF', 0.9280, 'BCE', TIME '13:15'),
           ('EUR', 'CAD', 1.6050, 'BCE', TIME '13:15'),
           ('EUR', 'AUD', 1.7780, 'BCE', TIME '13:15'),
           ('EUR', 'CNY', 8.2600, 'BCE', TIME '13:15'),
           ('EUR', 'MXN', 21.4000, 'BCE', TIME '13:15')
)
INSERT INTO cotacao (moeda_base_id, moeda_destino_id, valor, data_hora, fonte_id)
SELECT mb.id,
       md.id,
       round((b.valor * (1 + 0.02 * sin(d.n / 7.0)))::numeric, 6),
       -- nunca no futuro: no dia de hoje, no maximo agora - 1 minuto
       least(timezone('UTC', d.dia + b.hora), now() - INTERVAL '1 minute'),
       f.id
FROM dias d
         CROSS JOIN base b
         JOIN moeda mb ON mb.codigo_iso = b.base
         JOIN moeda md ON md.codigo_iso = b.destino
         JOIN fonte_dados f ON f.codigo = b.fonte
WHERE d.n < 90
ON CONFLICT DO NOTHING;

-- Indices: tres meses de historico; o mais recente com data de hoje (vence os calculados pelo worker).
INSERT INTO indice_geopolitico (pais_id, indice_risco, fator_ajuste, data_referencia, fonte_id)
SELECT p.id, i.indice, round(1 - i.indice / 100 * 0.05, 4), m.dia, f.id
FROM (VALUES ('BR', 30.00), ('US', 35.98), ('EU', 29.04), ('GB', 29.63), ('JP', 17.43), ('CH', 16.69),
             ('CA', 21.94), ('AU', 22.93), ('CN', 29.78), ('MX', 40.35), ('DE', 29.24), ('FR', 31.86),
             ('IT', 25.74), ('ES', 30.00), ('NL', 26.15)) AS i(pais, indice)
         CROSS JOIN (VALUES (current_date), (current_date - 30), (current_date - 60)) AS m(dia)
         JOIN pais p ON p.codigo_iso = i.pais
         JOIN fonte_dados f ON f.codigo = 'BANCO_MUNDIAL'
ON CONFLICT DO NOTHING;

-- Indicadores (ultimo ano completo) com valores das fixtures reais.
INSERT INTO indicador_economico (pais_id, tipo, valor, periodo, fonte_id)
SELECT p.id, x.tipo, x.valor, make_date(extract(year FROM current_date)::int - 1, 1, 1), f.id
FROM (VALUES ('BR', 'INFLACAO', 5.02), ('BR', 'JUROS', 13.75), ('BR', 'PIB', 2173665655937.27),
             ('US', 'INFLACAO', 2.95), ('US', 'JUROS', 4.25), ('US', 'PIB', 29184890000000),
             ('GB', 'INFLACAO', 3.88), ('JP', 'INFLACAO', 3.17), ('CH', 'INFLACAO', 0.15),
             ('CA', 'INFLACAO', 2.07), ('AU', 'INFLACAO', 2.87), ('CN', 'INFLACAO', 0.06),
             ('MX', 'INFLACAO', 3.81), ('MX', 'JUROS', 6.50),
             ('DE', 'INFLACAO', 2.17), ('DE', 'PIB', 5050922925047.05), ('FR', 'INFLACAO', 0.94),
             ('FR', 'PIB', 3366315927447.33), ('IT', 'INFLACAO', 1.53), ('IT', 'PIB', 2551556954100.35),
             ('ES', 'INFLACAO', 2.67), ('ES', 'PIB', 1906453309985.88), ('NL', 'INFLACAO', 3.20),
             ('NL', 'PIB', 1332767651100.39)) AS x(pais, tipo, valor)
         JOIN pais p ON p.codigo_iso = x.pais
         JOIN fonte_dados f ON f.codigo = 'BANCO_MUNDIAL'
ON CONFLICT DO NOTHING;
