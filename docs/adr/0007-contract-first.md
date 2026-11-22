# 0007: Contract-first: spec manual como fonte da verdade

## Contexto
A spec impressa no documento tinha YAML inválido (seção 5.10). A seção 4 pede spec escrita à mão, servida pelo springdoc, e teste que garanta que implementação e spec não divergem.

## Decisão
- `api-spec/openapi.yaml` (OpenAPI 3.0.3) reescrito e completado: 401/429/503 reutilizáveis em todos os endpoints; 404 e 400 conforme a 5.10; exemplos em todas as respostas; `X-Data-Referencia` documentado; Spectral (`spectral:oas` "all") sem avisos.
- **Entrada aceita minúsculas**: a 5.10 pede `^[A-Z]{3}$`, a 6.6 pede aceitar minúsculas e normalizar. Os parâmetros de entrada usam `^[A-Za-z]{3}$` / `^[A-Za-z]{2}$`; as respostas usam `^[A-Z]...$`. Assim a spec descreve fielmente o comportamento.
- `Moeda.pais` devolve o código ISO alfa-2 (não o nome), para encadear com `/indicadores/{pais}`.
- Restrição `dataFim >= dataInicio` e intervalo máximo de 366 dias: não são expressáveis em OpenAPI 3.0; ficam descritas no texto e são validadas no código (400).
- O Maven copia a spec para `static/api-spec/` e o Swagger UI (`/swagger-ui/index.html`) aponta para ela. O springdoc exige o endpoint de docs ligado; ele fica em `/interno/api-docs-gerado` e não é usado pelo UI.
- `ContratoApiTest` sobe a API real (Testcontainers) e valida 21 interações (requisição e resposta) com o `openapi-request-validator`.

## Consequências
Qualquer campo novo exige mudar a spec primeiro; o teste de contrato quebra se o código divergir.
