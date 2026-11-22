# AT05: Modelo de dados

Migrations em `db/migration` (Flyway, SQL puro). Testes em `modulos/dominio/.../MigracoesIntegracaoTest.java` (PostgreSQL limpo via Testcontainers).

```mermaid
erDiagram
    pais ||--o{ moeda : "emite"
    pais ||--o{ indicador_economico : "tem"
    pais ||--o{ indice_geopolitico : "tem"
    moeda ||--o{ cotacao : "base"
    moeda ||--o{ cotacao : "destino"
    moeda ||--o{ conversao : "origem"
    moeda ||--o{ conversao : "destino"
    fonte_dados ||--o{ cotacao : "publica"
    fonte_dados ||--o{ indicador_economico : "publica"
    fonte_dados ||--o{ indice_geopolitico : "base do cálculo"
    fonte_dados |o--o{ conversao : "fonte da taxa"

    pais {
        bigint id PK
        char2 codigo_iso UK "BR, US, EU"
        char3 codigo_iso3 UK "nulo para EU"
        varchar nome
        varchar regiao
    }
    moeda {
        bigint id PK
        char3 codigo_iso UK
        varchar nome
        varchar simbolo
        smallint casas_decimais "ISO 4217"
        bigint pais_id FK
    }
    fonte_dados {
        bigint id PK
        varchar codigo UK "BCB, BCE, FMI, BANCO_MUNDIAL"
        varchar nome
        varchar url_base
        varchar tipo
        boolean ativa
    }
    cotacao {
        bigint id PK
        timestamptz data_hora PK "chave de particao"
        bigint moeda_base_id FK
        bigint moeda_destino_id FK
        numeric_18_6 valor "maior que 0"
        bigint fonte_id FK
    }
    indicador_economico {
        bigint id PK
        bigint pais_id FK
        varchar tipo "INFLACAO JUROS PIB ESTABILIDADE_POLITICA"
        numeric_24_6 valor
        date periodo
        bigint fonte_id FK
    }
    indice_geopolitico {
        bigint id PK
        bigint pais_id FK
        numeric_5_2 indice_risco "0 a 100"
        numeric_6_4 fator_ajuste "maior que 0 e ate 1"
        date data_referencia
        bigint fonte_id FK
    }
    conversao {
        bigint id PK
        bigint moeda_origem_id FK
        bigint moeda_destino_id FK
        numeric valor_original
        numeric_18_6 taxa_aplicada
        numeric_6_4 fator_geopolitico
        numeric valor_convertido
        timestamptz data_hora
        bigint fonte_id FK "ADR 0014"
        char8 api_key_hash_prefixo "ADR 0014"
    }
```

## Migrations

| Arquivo | Conteúdo |
|---|---|
| `V1__schema_base.sql` | 7 tabelas, PK/FK, NOT NULL, CHECKs (risco 0–100, fator ∈ (0,1], valor > 0, tipo de indicador, padrões de código ISO); `cotacao` particionada por `data_hora` |
| `V2__particoes_cotacao.sql` | função `criar_particao_cotacao` (SECURITY DEFINER), partições de 24 meses antes a 3 depois, `DEFAULT` |
| `V3__indices.sql` | índices da especificação e unicidades de idempotência |
| `V4__dados_iniciais.sql` | 4 fontes, 15 países (10 com moeda + 5 da zona do euro), 10 moedas |
| `V5__papeis_permissoes.sql` | papéis `papel_api` e `papel_worker` com menor privilégio |

## Diferenças em relação ao modelo da Fase 2

| Mudança | Motivo | ADR |
|---|---|---|
| `TIMESTAMP` → `TIMESTAMPTZ` | ambiguidade de fuso | 0004 |
| PK de `cotacao` = `(id, data_hora)` | exigência de tabela particionada | 0012 |
| `moeda.casas_decimais` | arredondamento ISO 4217 | 0010 |
| `pais.codigo_iso3` | o FMI usa ISO alfa-3 | 0020 |
| `fonte_dados.codigo` | identificador estável usado pelo código | 0013 |
| `tipo = ESTABILIDADE_POLITICA` | guardar o PV bruto | 0005 |
| `conversao.fonte_id`, `api_key_hash_prefixo` | rastreabilidade | 0014 |
| Unicidades em indicador e índice | idempotência | 0013 |
