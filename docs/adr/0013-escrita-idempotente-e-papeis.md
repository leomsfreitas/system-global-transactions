# 0013: Escrita idempotente, migrations como tarefa e papéis do banco

## Decisão
- Toda gravação do worker é `INSERT ... ON CONFLICT DO NOTHING` em `GravacaoDados` (JdbcTemplate): histórico nunca sobrescrito, reexecução não duplica. Unicidades da V3: indicador `(pais, tipo, periodo, fonte)`, índice `(pais, data_referencia, fonte)`. JPA fica para leitura e para a auditoria.
- Três identidades: **dono** (roda migrations), **API** (`papel_api`: `SELECT` nas tabelas de dados, `SELECT, INSERT` em `conversao`) e **worker** (`papel_worker`: `SELECT, INSERT` em cotação, indicadores e índices; sem `UPDATE/DELETE`; sem acesso a `conversao`).
- A V5 cria só papéis `NOLOGIN` e os `GRANT`s. Usuários com senha são criados fora do versionamento: `docker/postgres/init/01-usuarios.sh` localmente e `infra/scripts/bootstrap-usuarios.sql` (executado uma vez por operador com senhas do Secrets Manager) na AWS.
- Migrations rodam como tarefa única (`--job=MigracaoJob` na imagem do worker) com o usuário dono, antes do deploy. API e worker sobem com `spring.flyway.enabled=false`.
- Migration extra `V5__papeis_permissoes.sql` além das V1–V4 pedidas.

## Consequências
`fonte_dados.ativa` é alterada por operador (o worker não tem `UPDATE`).
