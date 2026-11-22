# 0009: Pares gravados e taxa cruzada

## Contexto
O BCB publica X→BRL (PTAX) e o BCE publica EUR→X. Não há cotação direta para todos os pares (seção 5.4).

## Decisão
- Gravar exatamente o que a fonte publica: BCB `X→BRL` (`cotacaoVenda`, cada boletim), BCE `EUR→X`.
- Resolução (`ResolvedorTaxaCambio`, classe pura): par direto → inverso (1/valor) → cruzada via BRL com dados do BCB → cruzada via EUR com dados do BCE. As pernas da cruzada são da **mesma data UTC**; tenta-se o dia da perna mais recente de cada lado.
- Cada perna aceita o par gravado nos dois sentidos (ex.: via EUR, `EUR→BRL` vira BRL→EUR por inversão).
- Divisões com `MathContext(16, HALF_EVEN)`; taxa final com 6 casas.
- `fonte` na resposta: `BCB`, `BCE`, `Cruzada: BCB` ou `Cruzada: BCE`. Mesma moeda → taxa 1, fonte `Identidade`.
- A primeira candidata que respeita a regra de frescor vence; se todas estiverem velhas, 503 com a data da mais recente.
- Histórico: uma cotação por dia (a última), pelo mesmo encadeamento.

## Consequências
CNY e MXN só existem via BCE e sempre saem como `Cruzada: BCE` contra moedas que não o EUR. A cruzada exige até 4 consultas indexadas; o resultado fica em cache (ADR 0018).
