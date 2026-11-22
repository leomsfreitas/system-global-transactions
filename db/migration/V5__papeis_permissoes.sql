-- V5: papeis de menor privilegio (secao 6.2).
-- Tres identidades: dono (roda as migrations; e o usuario desta sessao), API e worker.
-- Aqui so existem papeis NOLOGIN com os GRANTs. Os usuarios com login e senha sao criados fora do
-- versionamento (docker/postgres/init e infra/scripts/bootstrap-usuarios.sql) e recebem o papel
-- com GRANT papel_x TO usuario. Assim nenhuma senha entra no repositorio.

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'papel_api') THEN
            CREATE ROLE papel_api NOLOGIN;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'papel_worker') THEN
            CREATE ROLE papel_worker NOLOGIN;
        END IF;
    END;
$$;

GRANT USAGE ON SCHEMA public TO papel_api, papel_worker;

-- API: leitura nas tabelas de dados, escrita so em conversao (auditoria).
GRANT SELECT ON pais, moeda, fonte_dados, cotacao, indicador_economico, indice_geopolitico TO papel_api;
GRANT SELECT, INSERT ON conversao TO papel_api;

-- Worker: INSERT nas tabelas de dados, sem UPDATE/DELETE (historico nunca e sobrescrito).
-- fonte_dados.ativa e alterada por operador, nao pelo worker. Sem acesso a conversao.
GRANT SELECT ON pais, moeda, fonte_dados TO papel_worker;
GRANT SELECT, INSERT ON cotacao, indicador_economico, indice_geopolitico TO papel_worker;
GRANT EXECUTE ON FUNCTION criar_particao_cotacao(DATE) TO papel_worker;
REVOKE EXECUTE ON FUNCTION criar_particao_cotacao(DATE) FROM PUBLIC;

-- Colunas IDENTITY usam sequencias implicitas.
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO papel_worker;
GRANT USAGE ON SEQUENCE conversao_id_seq TO papel_api;

-- Sem ALTER DEFAULT PRIVILEGES: INSERT/SELECT pela tabela-mae ja vale para particoes novas,
-- e tabelas futuras recebem GRANT explicito na migration que as criar (menor privilegio).
