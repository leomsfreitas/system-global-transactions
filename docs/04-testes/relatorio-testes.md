# AT10: Relatório de testes

Execução: `./mvnw verify` em 22/11/2026 (Java 21, Docker para Testcontainers). Resultado: **168 testes, 0 falhas**.

| Módulo | Testes | Cobertura de linhas (JaCoCo) | Destaques |
|---|---|---|---|
| dominio | 82 | 86,7% (meta 80%) | exemplo canônico 533,97; arredondamento HALF_EVEN; propriedade "com ajuste ≤ sem ajuste" (5.000 casos); taxa direta, inversa e cruzada; frescor; migrations e partições no PostgreSQL real; cache com Redis fora e stampede |
| conectores | 40 | 90,1% (meta 80%) | BCB, BCE, Banco Mundial e FMI com respostas reais (WireMock): sucesso, vazio, 500, timeout, JSON malformado, circuit breaker |
| api | 37 | não exigida | teste de contrato (21 cenários contra `api-spec/openapi.yaml`), 400/401/404/405/503, cabeçalhos de segurança, API sem Redis |
| worker | 9 | não exigida | jobs contra WireMock + PostgreSQL + Redis: idempotência, invalidação de cache, falha isolada de fonte, fonte desativada, índice do EU |

Cobertura medida sem as classes de entidade (`modelo`) e de configuração (`config`).

## Cenários de falha exigidos
| Cenário | Teste | Resultado |
|---|---|---|
| Fonte externa fora | `JobsIntegracaoTest.falhaDeUmaFonte...`, `*ClienteTest.erro500...` | falha contada, outras fontes seguem, nada é apagado |
| Redis fora | `RedisForaTest`, `CacheRedisTest` | API responde pelo PostgreSQL |
| Dado desatualizado | `ApiIntegracaoTest.dadoDesatualizado_503`, `ResolvedorTaxaCambioTest` | 503 com a data da cotação |
| Chave inválida | `ApiIntegracaoTest.chaveAusenteOuInvalida_401...` | 401 no formato `Erro` |

## Verificação manual (make up)
Os 6 endpoints responderam com os dados do seed; o worker gravou dados do WireMock (20 cotações BCB, 34 indicadores, 15 índices); `POST /conversoes` 100 USD→BRL retornou 533,97.
