# 0003: Mapeamento manual entre domínio e DTO

## Contexto
A seção 4 permite MapStruct ou mapeamento manual, desde que se escolha um.

## Decisão
Mapeamento manual em `api/web/Mapeador.java`. São 5 DTOs de resposta, todos records com poucos campos.

## Alternativas
MapStruct: gera código em tempo de compilação, mas adiciona um processador de anotações e mais uma dependência para 5 conversões triviais.

## Consequências
Conversões explícitas e fáceis de testar; normalizações (UTC, `stripTrailingZeros` nos indicadores, formato do `X-Data-Referencia`) ficam num lugar só.
