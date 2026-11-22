# 0004: TIMESTAMPTZ em vez de TIMESTAMP

## Contexto
O modelo da Fase 2 usa `TIMESTAMP`. As fontes publicam em fusos diferentes (PTAX em horário de Brasília sem fuso explícito; BCE às 14h15 de Frankfurt) e a API responde em UTC.

## Decisão
Todas as colunas de data e hora são `TIMESTAMPTZ`. O conector converte para o instante correto na entrada (PTAX: `America/Sao_Paulo`; BCE: `Europe/Berlin` às 14h15); Hibernate grava em UTC (`hibernate.jdbc.time_zone=UTC`); a API responde ISO 8601 em UTC com segundos sempre presentes.

## Alternativas
`TIMESTAMP` assumindo UTC por convenção: ambíguo e sujeito a erro quando alguém grava horário local.

## Consequências
Partições mensais têm fronteira em UTC (ADR 0012). "Mesma data" na taxa cruzada é o dia UTC.
