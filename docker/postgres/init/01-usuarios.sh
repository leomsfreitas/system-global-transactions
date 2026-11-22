#!/bin/sh
# Cria os usuarios de login da API e do worker (senhas vindas do .env, nunca do repositorio).
# O dono (POSTGRES_USER) roda as migrations; a V5 da os GRANTs aos papeis.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'papel_api') THEN CREATE ROLE papel_api NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'papel_worker') THEN CREATE ROLE papel_worker NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transacoes_api') THEN
    CREATE ROLE transacoes_api LOGIN PASSWORD '${DB_SENHA_API}' IN ROLE papel_api;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transacoes_worker') THEN
    CREATE ROLE transacoes_worker LOGIN PASSWORD '${DB_SENHA_WORKER}' IN ROLE papel_worker;
  END IF;
END
\$\$;
SQL
