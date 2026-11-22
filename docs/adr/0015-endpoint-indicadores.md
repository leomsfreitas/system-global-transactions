# 0015: Endpoint de indicadores devolve o valor mais recente por tipo

## Contexto
`GET /indicadores/{pais}` deve retornar "indicadores econômicos do país", sem dizer se é série ou último valor.

## Decisão
Devolve uma lista com o **valor mais recente de cada tipo** (`INFLACAO`, `JUROS`, `PIB`), cada um com seu `periodo`. Empate de período entre fontes: o último gravado. País inexistente ou sem indicadores: 404 com mensagens distintas. `ESTABILIDADE_POLITICA` não é exposto.

## Alternativas
Série completa: payload grande e sem filtro no contrato. Um filtro `tipo` ou um endpoint de série exigiriam mudança de contrato (permitida só com ADR e compatível) e ficam como evolução.

## Consequências
Resposta pequena e cacheável (`indicadores:{PAIS}`, TTL 24 h).
