# 0014: Auditoria de conversão

## Decisão
- Toda chamada bem-sucedida a `POST /conversoes` grava uma linha em `conversao`, de forma síncrona.
- Proposta opcional da seção 5.11 **adotada**: colunas `fonte_id` (fonte da taxa; na cruzada, a do pivô) e `api_key_hash_prefixo` (8 primeiros caracteres do SHA-256 da chave). A chave nunca é gravada.
- Medição de impacto no teste de carga (ver `docs/04-testes/relatorio-carga.md`). Plano B se virar gargalo: fila em memória com escrita em lote assíncrona, aceitando perda em caso de queda da instância (documentar antes de adotar).

## Consequências
Rastreabilidade por cliente sem guardar segredo; a tabela cresce linearmente com o tráfego (índice por `data_hora` para expurgo futuro).
