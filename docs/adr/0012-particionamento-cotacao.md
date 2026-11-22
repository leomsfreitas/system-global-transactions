# 0012: Particionamento mensal de cotação e PK composta

## Decisão
- `cotacao` criada já como `PARTITION BY RANGE (data_hora)` na V1; PK `(id, data_hora)`; único `(moeda_base_id, moeda_destino_id, data_hora)` (ambos incluem a coluna de partição, exigência do PostgreSQL).
- V2 cria partições mensais dos 24 meses anteriores, do mês corrente e dos 3 seguintes (relativo à data da migration, fronteiras em UTC) e uma partição `DEFAULT`.
- Função `criar_particao_cotacao(date)` `SECURITY DEFINER` (roda como dono da tabela): o worker só tem `EXECUTE` nela, sem privilégio de DDL. `ManutencaoParticoesJob` a chama todo dia para o mês corrente e os 3 seguintes (idempotente).
- Entidade JPA `Cotacao` com `@IdClass(CotacaoId)` e `@Immutable` (leitura); a escrita é SQL nativo (ADR 0013).

## Consequências
Se dados caírem na `DEFAULT` num intervalo e depois se tentar criar a partição desse mês, o PostgreSQL recusa; a criação antecipada de 3 meses evita o caso.
