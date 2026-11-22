# AT11: Relatório de carga

Ambiente: `make up` num notebook (Docker com 10 CPUs e 8 GB), uma instância da API, k6 2.3.0 na mesma rede Docker. Não representa o ECS Fargate; serve para validar a aplicação.

## Carga: 500 req/s por 10 minutos (`testes-carga/carga.js`)
Mistura: 50% cotação, 30% conversão (com auditoria síncrona), 20% moedas, índices e indicadores.

| Métrica | Meta | Resultado |
|---|---|---|
| Requisições | 500 req/s por 10 min | 300.001 (499,99 req/s), 0 iterações descartadas |
| Taxa de erro | < 1% | **0,00%** |
| p95 `GET /cotacoes` (cache quente) | < 100 ms | **0,92 ms** (p99 2,18 ms) |
| p95 `POST /conversoes` | < 300 ms | **2,59 ms** (p99 5,3 ms) |

Após o teste: API com 453 MiB de memória; PostgreSQL e Redis ociosos. A gravação síncrona da auditoria (ADR 0014) não virou gargalo, então não foi preciso torná-la assíncrona. Nenhum ajuste de pool, TTL ou índice foi necessário.

## Estresse e soak
Os scripts estão prontos (`testes-carga/estresse.js`, até 4.000 req/s, e `testes-carga/soak.js`, 30 min), mas não foram executados. Com a meta atingida com folga, a equipe decidiu não rodá-los, já que o sistema é acadêmico. Para rodar: `make stress-test` e `make soak-test`.
