# AT04: Arquitetura técnica da API e da infraestrutura

Desenho da Fase 2 transformado em artefato versionado. Decisões em `docs/adr/`.

## 1. Contexto (C4 nível 1)

```mermaid
flowchart LR
    cliente["Cliente YouPay<br/>(sistema que converte valores)"]
    operador["Operador / time de engenharia"]
    sistema["Sistema de transações com<br/>ajuste geopolítico"]
    bcb["Banco Central do Brasil<br/>PTAX (Olinda) e SGS"]
    bce["Banco Central Europeu<br/>ECB Data Portal (EXR)"]
    bm["Banco Mundial<br/>API v2 (WDI, WGI)"]
    fmi["FMI<br/>SDMX 3.0"]

    cliente -- "HTTPS + X-API-Key<br/>conversões, cotações, índices" --> sistema
    operador -- "alarmes, dashboards,<br/>fonte_dados.ativa" --> sistema
    sistema -- "cotações X→BRL, Selic, IPCA" --> bcb
    sistema -- "cotações EUR→X" --> bce
    sistema -- "inflação, PIB, estabilidade política" --> bm
    sistema -- "inflação e juros (secundária)" --> fmi
```

## 2. Contêineres (C4 nível 2)

```mermaid
flowchart TB
    cliente["Cliente"]
    subgraph aws["AWS (região sa-east-1)"]
        waf["AWS WAF<br/>regras gerenciadas"]
        apigw["API Gateway REST<br/>chave de API, usage plans,<br/>throttling, domínio + ACM"]
        vpclink["VPC Link v2"]
        subgraph vpc["VPC (2 AZs)"]
            subgraph privada["Sub-redes privadas"]
                alb["ALB interno"]
                api["API<br/>Spring Boot (ECS Fargate)<br/>controllers, motor de conversão"]
                worker["Worker<br/>Spring Boot (tarefas ECS)<br/>conectores e jobs"]
                redis[("ElastiCache Redis<br/>TLS, réplica em outra AZ")]
                rds[("RDS PostgreSQL Multi-AZ<br/>KMS, TLS obrigatório")]
            end
            nat["NAT Gateway<br/>(sub-rede pública)"]
        end
        eb["EventBridge Scheduler"]
        sm["Secrets Manager"]
        cw["CloudWatch<br/>logs, métricas, alarmes → SNS"]
        ecr["ECR"]
    end
    fontes["Fontes externas<br/>BCB, BCE, Banco Mundial, FMI"]

    cliente --> waf --> apigw --> vpclink --> alb --> api
    api -- "cache hit" --> redis
    api -- "cache miss, auditoria" --> rds
    eb -- "RunTask --job=NOME" --> worker
    worker --> rds
    worker -- "invalida chaves" --> redis
    worker -- "HTTPS via NAT" --> nat --> fontes
    api -.-> sm
    worker -.-> sm
    api -.-> cw
    worker -.-> cw
    ecr -.-> api
    ecr -.-> worker
```

| Contêiner | Tecnologia | Responsabilidade |
|---|---|---|
| API (`modulos/api`) | Java 21, Spring Boot 4.1, Spring MVC, virtual threads | 6 endpoints, validação, filtros de chave e limite, cache, auditoria |
| Domínio (`modulos/dominio`) | biblioteca | entidades, repositórios, `MotorConversao`, estratégias, `ResolvedorTaxaCambio`, cache |
| Conectores (`modulos/conectores`) | WebClient + Resilience4j | clientes BCB (PTAX, SGS), BCE, Banco Mundial, FMI |
| Worker (`modulos/worker`) | Spring Boot sem web | 5 jobs + migração, um por execução de tarefa ECS |
| Banco | RDS PostgreSQL 17 | 7 tabelas, `cotacao` particionada por mês |
| Cache | ElastiCache Redis 7 | cotações, índices, indicadores, moedas |

## 3. Sequência: conversão com ajuste

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant G as API Gateway
    participant A as API (ECS)
    participant R as Redis
    participant P as PostgreSQL
    C->>G: POST /api/v1/conversoes + X-API-Key
    G->>G: valida chave, usage plan, WAF
    G->>A: VPC Link v2 → ALB interno
    A->>A: FiltroChaveApi (SHA-256), validação do corpo
    A->>R: GET moedas:lista
    alt cache miss ou Redis fora
        A->>P: SELECT moedas
        A-->>R: SET moedas:lista (24 h)
    end
    A->>R: GET cotacao:atual:USD:BRL
    alt cache miss
        A->>P: par direto → inverso → cruzada BRL/BCB → cruzada EUR/BCE
        A-->>R: SET (15 min)
    end
    A->>A: regra de frescor (≤ 4 dias) senão 503
    A->>R: GET indice:atual:BR (estratégia PAIS_DESTINO)
    alt cache miss
        A->>P: índice mais recente do país
        A-->>R: SET (24 h)
    end
    A->>A: MotorConversao: 100,00 × 5,421000 × 0,9850 = 533,97
    A->>P: INSERT conversao (auditoria)
    A-->>C: 200 ConversaoResponse + X-Data-Referencia
```

## 4. Sequência: atualização de cotações (worker)

```mermaid
sequenceDiagram
    participant E as EventBridge Scheduler
    participant W as Worker (tarefa ECS)
    participant F as Fonte (BCB/BCE)
    participant P as PostgreSQL
    participant R as Redis
    participant M as CloudWatch
    E->>W: RunTask --job=AtualizacaoCotacoesBcbJob
    W->>P: fonte_dados.ativa?
    W->>F: GET (timeout, retry com backoff, circuit breaker)
    F-->>W: JSON (valida valor, data, moeda; descarta inválidos)
    W->>P: INSERT ... ON CONFLICT DO NOTHING
    W->>R: apaga cotacao:atual:*
    W->>M: métricas (lidos, gravados, ignorados, descartados, falhas, duração)
    W-->>E: encerra com código 0 ou 1
```

## 5. Rede e segurança

```mermaid
flowchart LR
    subgraph az1["AZ a"]
        pub1["pública: NAT"]
        app1["privada app: API, worker, ALB"]
        dados1["privada dados: RDS primário, Redis primário"]
    end
    subgraph az2["AZ b"]
        pub2["pública: (NAT só em prod)"]
        app2["privada app: API, worker, ALB"]
        dados2["privada dados: RDS standby, Redis réplica"]
    end
    internet((Internet)) --> gw["API Gateway + WAF"] --> link["VPC Link v2"] --> app1 & app2
    app1 & app2 -- "5432 / 6379 (SG)" --> dados1 & dados2
    app1 -- "443 saída" --> pub1 --> internet
```

| Security group | Entrada permitida |
|---|---|
| `vpclink` | nenhuma (só saída para o ALB) |
| `alb` | 80 do SG `vpclink` |
| `api` | 8080 do SG `alb` |
| `worker` | nenhuma |
| `rds` | 5432 dos SGs `api` e `worker` |
| `redis` | 6379 dos SGs `api` e `worker` |

Autoscaling da API por CPU (alvo 60%, mínimo 2 tarefas em prod). Código em `infra/`.
