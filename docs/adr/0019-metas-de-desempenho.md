# 0019: Metas de desempenho

## Decisão
Metas padrão da seção 5.12 adotadas para a AT11:
- `GET /cotacoes/{base}/{destino}` com cache quente: p95 < 100 ms.
- `POST /conversoes`: p95 < 300 ms.
- 500 req/s sustentadas por 10 min com erro < 1%.
- Estresse até o ponto de quebra, documentado; soak de 30 min.

## Consequências
Medido em ambiente local (Docker no notebook), que não representa o ECS Fargate; o relatório separa limites da máquina de limites da aplicação.
