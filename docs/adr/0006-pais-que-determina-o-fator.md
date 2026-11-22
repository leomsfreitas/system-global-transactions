# 0006: País que determina o fator geopolítico

## Contexto
O documento não diz se o fator é do país da moeda de origem, de destino ou dos dois (seção 5.2).

## Decisão
Interface `EstrategiaAjusteGeopolitico` com três implementações, escolhida por `ajuste-geopolitico.estrategia`:

| Valor | Classe | Regra |
|---|---|---|
| `PAIS_DESTINO` (padrão) | `AjustePaisDestino` | fator do país da moeda de destino |
| `PAIS_ORIGEM` | `AjustePaisOrigem` | fator do país da moeda de origem |
| `MENOR_FATOR` | `AjusteMenorFator` | o menor dos dois (o mais conservador) |

País sem índice → 503 (dado indisponível), nunca fator 1 silencioso.

## Consequências
Trocar a regra é configuração. O exemplo canônico (USD→BRL, fator do BR 0,9850) vale para o padrão.
