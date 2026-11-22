# 0018: Cache Redis

## Decisão
- Chaves da seção 6.4: `cotacao:atual:{BASE}:{DESTINO}` (taxa resolvida, inclusive cruzada), `indice:atual:{PAIS}`, `moedas:lista`, `indicadores:{PAIS}`. Valores JSON (Jackson 3), via `StringRedisTemplate` (Lettuce).
- TTL (`cache.*`): cotação 15 min; índice, indicadores e moedas 24 h.
- Invalidação ativa: após gravar dados novos, o worker apaga por prefixo (`SCAN` + `DEL`): cotações apagam todo `cotacao:atual:*` (qualquer par pode depender delas via cruzada).
- **Redis é otimização, não dependência**: qualquer erro de Redis cai no banco; após uma falha o Redis é ignorado por `cache.pausa-apos-falha` (30 s) para não somar o timeout (200 ms) a cada requisição. Teste `RedisForaTest`.
- **Cache stampede**: recomputação única por chave e por instância (single flight com `CompletableFuture`). Sem lock distribuído: no pior caso cada instância recalcula uma vez.

## Consequências
Valores de cache com cotação envelhecida são revalidados (ADR 0011).
