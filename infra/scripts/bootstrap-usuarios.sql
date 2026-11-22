-- Executar UMA vez no RDS, conectado como transacoes_owner, depois da primeira migracao.
-- As senhas vem dos segredos <ambiente>/banco/api e <ambiente>/banco/worker do Secrets Manager:
--   psql "$URL" -v senha_api="$(aws secretsmanager get-secret-value ... | jq -r .senha)" -v senha_worker=... -f bootstrap-usuarios.sql
-- Nunca grave as senhas neste arquivo.
CREATE ROLE transacoes_api LOGIN PASSWORD :'senha_api' IN ROLE papel_api;
CREATE ROLE transacoes_worker LOGIN PASSWORD :'senha_worker' IN ROLE papel_worker;
