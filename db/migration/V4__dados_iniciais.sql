-- V4: dados de referencia (fontes, paises, moedas). Lista confirmada na AT01 (docs/01-iniciacao/escopo-fontes.md).
-- ARS ficou fora: nenhuma fonte publica cotacao atual (risco R02).

INSERT INTO fonte_dados (codigo, nome, url_base, tipo, ativa)
VALUES ('BCB', 'Banco Central do Brasil', 'https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata', 'COTACAO_E_INDICADOR', TRUE),
       ('BCE', 'Banco Central Europeu', 'https://data-api.ecb.europa.eu/service/data', 'COTACAO', TRUE),
       ('FMI', 'Fundo Monetario Internacional', 'https://api.imf.org/external/sdmx/3.0', 'INDICADOR', TRUE),
       ('BANCO_MUNDIAL', 'Banco Mundial', 'https://api.worldbank.org/v2', 'INDICADOR', TRUE);

INSERT INTO pais (codigo_iso, codigo_iso3, nome, regiao)
VALUES ('BR', 'BRA', 'Brasil', 'America do Sul'),
       ('US', 'USA', 'Estados Unidos', 'America do Norte'),
       -- Codigo reservado ISO 3166 para a Uniao Europeia (ADR 0008)
       ('EU', NULL, 'Uniao Europeia', 'Europa'),
       ('GB', 'GBR', 'Reino Unido', 'Europa'),
       ('JP', 'JPN', 'Japao', 'Asia'),
       ('CH', 'CHE', 'Suica', 'Europa'),
       ('CA', 'CAN', 'Canada', 'America do Norte'),
       ('AU', 'AUS', 'Australia', 'Oceania'),
       ('CN', 'CHN', 'China', 'Asia'),
       ('MX', 'MEX', 'Mexico', 'America do Norte'),
       -- Zona do euro: compoem o indice do EU (media ponderada pelo PIB)
       ('DE', 'DEU', 'Alemanha', 'Europa'),
       ('FR', 'FRA', 'Franca', 'Europa'),
       ('IT', 'ITA', 'Italia', 'Europa'),
       ('ES', 'ESP', 'Espanha', 'Europa'),
       ('NL', 'NLD', 'Paises Baixos', 'Europa');

INSERT INTO moeda (codigo_iso, nome, simbolo, casas_decimais, pais_id)
SELECT m.codigo, m.nome, m.simbolo, m.casas, p.id
FROM (VALUES ('BRL', 'Real brasileiro', 'R$', 2, 'BR'),
             ('USD', 'Dolar americano', 'US$', 2, 'US'),
             ('EUR', 'Euro', '€', 2, 'EU'),
             ('GBP', 'Libra esterlina', '£', 2, 'GB'),
             ('JPY', 'Iene', '¥', 0, 'JP'),
             ('CHF', 'Franco suico', 'CHF', 2, 'CH'),
             ('CAD', 'Dolar canadense', 'C$', 2, 'CA'),
             ('AUD', 'Dolar australiano', 'A$', 2, 'AU'),
             ('CNY', 'Yuan renminbi', 'CN¥', 2, 'CN'),
             ('MXN', 'Peso mexicano', 'MX$', 2, 'MX')) AS m(codigo, nome, simbolo, casas, pais)
         JOIN pais p ON p.codigo_iso = m.pais;
