# 0011: Frescor dos dados e agendamento das fontes

## Contexto
As fontes não são de tempo real (seção 5.6). A AT01 mediu: PTAX com boletins das 10h às 13h30 de Brasília em dias úteis; BCE uma vez por dia útil (~16h CET); indicadores anuais ou mensais.

## Decisão
| Job | EventBridge (AWS) | `@Scheduled` local |
|---|---|---|
| `AtualizacaoCotacoesBcbJob` | `cron(0/15 9-17 ? * MON-FRI *)` em America/Sao_Paulo | `0 */15 9-17 * * MON-FRI` |
| `AtualizacaoCotacoesBceJob` | `cron(30 17 ? * MON-FRI *)` em Europe/Berlin | `0 30 17 * * MON-FRI` |
| `AtualizacaoIndicadoresJob` | `cron(0 6 * * ? *)` UTC | `0 0 6 * * *` |
| `CalculoIndiceGeopoliticoJob` | `cron(30 6 * * ? *)` UTC | `0 30 6 * * *` |
| `ManutencaoParticoesJob` | `cron(0 3 * * ? *)` UTC | `0 0 3 * * *` |

- Cada execução coleta os últimos `worker.janela-cotacoes-dias` (5) dias, preenchendo lacunas de execuções perdidas sem duplicar (ADR 0013).
- **Regra de frescor**: cotação mais antiga que `cotacao.idade-maxima` (4 dias) → 503 com mensagem clara. Cobre fim de semana prolongado.
- Cabeçalho `X-Data-Referencia` (ISO 8601 UTC) com a data da cotação usada, em `/cotacoes/{base}/{destino}` e `/conversoes`.
- Valor de cache que envelheceu é descartado e recalculado antes de responder.
- No perfil local o worker roda todos os jobs uma vez ao iniciar (`worker.executar-ao-iniciar`).

## Consequências
A janela 9h–18h do BCB tem chamadas ociosas à tarde; mantida por ser o padrão pedido e configurável. Alarme "dado desatualizado" usa a métrica `worker.job.ultimo.sucesso`.
