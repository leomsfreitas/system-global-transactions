# Sistema de transações com ajuste geopolítico

API REST que converte valores entre moedas com cotações do BCB e do BCE e aplica um fator de ajuste pelo risco geopolítico do país (Banco Mundial e FMI). Projeto da disciplina GCSW (patrocínio YouPay).

## Pré-requisitos
- Java 21 e Docker (as demais ferramentas rodam em contêiner).

## Subir localmente
```bash
make up      # postgres, redis, wiremock (fontes simuladas), migração, seed de 90 dias, api e worker
make down    # derruba e apaga os dados
```
API em `http://localhost:8080`, Swagger UI em `http://localhost:8080/swagger-ui/index.html`.
Chave local de exemplo: **`chave-local-de-exemplo`** (o `.env` guarda só o hash SHA-256).

## Exemplos (curl)
```bash
K='X-API-Key: chave-local-de-exemplo'
curl -X POST localhost:8080/api/v1/conversoes -H "$K" -H 'Content-Type: application/json' \
     -d '{"moedaOrigem":"USD","moedaDestino":"BRL","valor":100.00,"aplicarAjusteGeopolitico":true}'
# -> valorConvertido 533.97 (taxa 5.4210 x fator 0.9850)
curl localhost:8080/api/v1/moedas -H "$K"
curl localhost:8080/api/v1/cotacoes/USD/BRL -H "$K"
curl "localhost:8080/api/v1/cotacoes/USD/BRL/historico?dataInicio=2026-11-01&dataFim=2026-11-20" -H "$K"
curl localhost:8080/api/v1/indicadores/BR -H "$K"
curl localhost:8080/api/v1/indices-geopoliticos/BR -H "$K"
```

## Comandos
| Comando | O que faz |
|---|---|
| `make test` / `make verify` | testes (Testcontainers), cobertura JaCoCo e Spotless |
| `make lint` | Spectral, Spotless, Terraform (fmt/validate/tflint), actionlint |
| `make seguranca` / `make zap` | Gitleaks, Trivy, Checkov / OWASP ZAP |
| `make load-test` | k6, 500 req/s por 10 min |
| `make plan AMBIENTE=prod` | `terraform plan` (precisa de credenciais AWS) |

## Estrutura
`api-spec/` contrato OpenAPI · `modulos/` (dominio, conectores, api, worker) · `db/` migrations e seed · `infra/` Terraform · `testes-carga/` k6 · `docs/` gestão, arquitetura, ADRs, testes, guia do desenvolvedor.
