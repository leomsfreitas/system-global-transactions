# Visão geral do código (AT07 a AT09)

| Módulo | Pacotes principais |
|---|---|
| `dominio` | `motor` (MotorConversao, CalculadoraIndiceGeopolitico), `estrategia`, `taxa` (ResolvedorTaxaCambio), `cache` (CacheRedis), `servico`, `repositorio`, `modelo` |
| `conectores` | `bcb` (PTAX, SGS), `bce`, `bancomundial`, `fmi`, `http` (ClienteHttpResiliente, SdmxJson, ValidadorDados), `config` |
| `api` | `web` (controllers, TratadorErros, DTOs), `seguranca` (filtros), `config` |
| `worker` | `job` (5 jobs + MigracaoJob), `execucao` (ExecutorJobs, ExecucaoPorArgumento, AgendadorLocal) |

## Como rodar
- `make up`: ambiente local completo; `make test`/`make verify`: testes; `make lint`: Spectral, Spotless, Terraform, actionlint.
- Worker como tarefa: `java -jar worker.jar --job=AtualizacaoCotacoesBcbJob` (encerra com código 0/1).

## Convenções
- Dinheiro sempre `BigDecimal` / `NUMERIC`; nenhuma ocorrência de `double`/`float` em valores monetários (verificado na AT12).
- Classes e mensagens em português; Conventional Commits com escopo `ATxx`.
