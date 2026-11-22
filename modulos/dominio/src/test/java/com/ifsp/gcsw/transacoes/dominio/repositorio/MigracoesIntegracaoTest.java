package com.ifsp.gcsw.transacoes.dominio.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.dominio.ContainersTeste;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** AT05: migrations V1 a V5 aplicadas em PostgreSQL limpo; particionamento e restricoes. */
@SpringBootTest
@Import(ContainersTeste.class)
class MigracoesIntegracaoTest {

  @Autowired JdbcTemplate jdbc;
  @Autowired GravacaoDados gravacao;

  @Test
  void criaAsSeteTabelas() {
    List<String> tabelas =
        jdbc.queryForList(
            """
            SELECT tablename FROM pg_tables
            WHERE schemaname = 'public' AND tablename NOT LIKE 'cotacao\\_%' AND tablename <> 'flyway_schema_history'
            ORDER BY 1
            """,
            String.class);
    assertThat(tabelas)
        .containsExactly(
            "conversao",
            "cotacao",
            "fonte_dados",
            "indicador_economico",
            "indice_geopolitico",
            "moeda",
            "pais");
  }

  @Test
  void cotacaoEParticionadaPorMes_com24AnterioresMaisCorrenteMais3SeguintesEDefault() {
    String estrategia =
        jdbc.queryForObject(
            "SELECT partstrat FROM pg_partitioned_table WHERE partrelid = 'cotacao'::regclass",
            String.class);
    assertThat(estrategia).isEqualTo("r");

    List<String> particoes =
        jdbc.queryForList(
            "SELECT c.relname FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid "
                + "WHERE i.inhparent = 'cotacao'::regclass ORDER BY 1",
            String.class);
    java.time.YearMonth corrente = java.time.YearMonth.now(java.time.ZoneOffset.UTC);
    java.time.format.DateTimeFormatter formato =
        java.time.format.DateTimeFormatter.ofPattern("yyyy_MM");
    List<String> esperadas =
        java.util.stream.IntStream.rangeClosed(-24, 3)
            .mapToObj(i -> "cotacao_" + corrente.plusMonths(i).format(formato))
            .toList();
    // 24 anteriores + corrente + 3 seguintes + DEFAULT (outros testes podem criar meses adicionais)
    assertThat(esperadas).hasSize(28);
    assertThat(particoes).containsAll(esperadas).contains("cotacao_default");
  }

  @Test
  void pkDeCotacaoIncluiAColunaDeParticao() {
    List<String> colunas =
        jdbc.queryForList(
            """
            SELECT a.attname FROM pg_index ix
            JOIN pg_attribute a ON a.attrelid = ix.indrelid AND a.attnum = ANY(ix.indkey)
            WHERE ix.indrelid = 'cotacao'::regclass AND ix.indisprimary ORDER BY 1
            """,
            String.class);
    assertThat(colunas).containsExactly("data_hora", "id");
  }

  @Test
  void linhaVaiParaAParticaoDoMes_eForaDaJanelaVaiParaDefault() {
    jdbc.update(
        "INSERT INTO cotacao (moeda_base_id, moeda_destino_id, valor, data_hora, fonte_id) "
            + "SELECT b.id, d.id, 1.5, now(), f.id FROM moeda b, moeda d, fonte_dados f "
            + "WHERE b.codigo_iso = 'CHF' AND d.codigo_iso = 'CAD' AND f.codigo = 'BCB'");
    jdbc.update(
        "INSERT INTO cotacao (moeda_base_id, moeda_destino_id, valor, data_hora, fonte_id) "
            + "SELECT b.id, d.id, 1.5, '2099-01-01T00:00:00Z', f.id FROM moeda b, moeda d, fonte_dados f "
            + "WHERE b.codigo_iso = 'CHF' AND d.codigo_iso = 'CAD' AND f.codigo = 'BCB'");

    List<String> destinos =
        jdbc.queryForList(
            "SELECT c.tableoid::regclass::text FROM cotacao c JOIN moeda b ON b.id = c.moeda_base_id "
                + "JOIN moeda d ON d.id = c.moeda_destino_id "
                + "WHERE b.codigo_iso = 'CHF' AND d.codigo_iso = 'CAD' ORDER BY data_hora",
            String.class);
    assertThat(destinos.get(0)).matches("cotacao_\\d{4}_\\d{2}");
    assertThat(destinos.get(1)).isEqualTo("cotacao_default");
  }

  @Test
  void funcaoDeParticaoEIdempotente() {
    LocalDate mes = LocalDate.now().plusMonths(6);
    String primeira = gravacao.criarParticaoCotacao(mes);
    String segunda = gravacao.criarParticaoCotacao(mes);
    assertThat(primeira).isEqualTo(segunda).startsWith("cotacao_");
    assertThat(jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, primeira))
        .isTrue();
  }

  @Test
  void restricoesDeIndiceGeopolitico() {
    assertThatThrownBy(() -> inserirIndice("100.01", "0.9500"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> inserirIndice("-0.01", "0.9500"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> inserirIndice("10", "0"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> inserirIndice("10", "1.0001"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void restricoesDeCotacaoEIndicador() {
    assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO cotacao (moeda_base_id, moeda_destino_id, valor, data_hora, fonte_id) "
                    + "SELECT b.id, d.id, 0, now(), f.id FROM moeda b, moeda d, fonte_dados f "
                    + "WHERE b.codigo_iso = 'USD' AND d.codigo_iso = 'BRL' AND f.codigo = 'BCB'"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO indicador_economico (pais_id, tipo, valor, periodo, fonte_id) "
                    + "SELECT p.id, 'DESEMPREGO', 1, current_date, f.id FROM pais p, fonte_dados f "
                    + "WHERE p.codigo_iso = 'BR' AND f.codigo = 'BCB'"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void dadosIniciais_fontesPaisesEMoedas() {
    assertThat(jdbc.queryForList("SELECT codigo FROM fonte_dados ORDER BY 1", String.class))
        .containsExactly("BANCO_MUNDIAL", "BCB", "BCE", "FMI");
    assertThat(jdbc.queryForList("SELECT codigo_iso FROM moeda ORDER BY 1", String.class))
        .containsExactly("AUD", "BRL", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "MXN", "USD");
    assertThat(
            jdbc.queryForObject(
                "SELECT casas_decimais FROM moeda WHERE codigo_iso = 'JPY'", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT p.codigo_iso FROM moeda m JOIN pais p ON p.id = m.pais_id WHERE m.codigo_iso = 'EUR'",
                String.class))
        .isEqualTo("EU");
  }

  @Test
  void papeisDeMenorPrivilegio() {
    assertThat(privilegio("papel_api", "cotacao", "SELECT")).isTrue();
    assertThat(privilegio("papel_api", "cotacao", "INSERT")).isFalse();
    assertThat(privilegio("papel_api", "conversao", "INSERT")).isTrue();
    assertThat(privilegio("papel_api", "conversao", "UPDATE")).isFalse();
    assertThat(privilegio("papel_worker", "cotacao", "INSERT")).isTrue();
    assertThat(privilegio("papel_worker", "cotacao", "UPDATE")).isFalse();
    assertThat(privilegio("papel_worker", "cotacao", "DELETE")).isFalse();
    assertThat(privilegio("papel_worker", "conversao", "SELECT")).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT has_function_privilege('papel_worker', 'criar_particao_cotacao(date)', 'EXECUTE')",
                Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT has_function_privilege('papel_api', 'criar_particao_cotacao(date)', 'EXECUTE')",
                Boolean.class))
        .isFalse();
  }

  private boolean privilegio(String papel, String tabela, String acao) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT has_table_privilege(?, ?, ?)", Boolean.class, papel, tabela, acao));
  }

  private void inserirIndice(String indice, String fator) {
    jdbc.update(
        "INSERT INTO indice_geopolitico (pais_id, indice_risco, fator_ajuste, data_referencia, fonte_id) "
            + "SELECT p.id, CAST(? AS NUMERIC), CAST(? AS NUMERIC), current_date, f.id FROM pais p, fonte_dados f "
            + "WHERE p.codigo_iso = 'BR' AND f.codigo = 'BANCO_MUNDIAL'",
        indice,
        fator);
  }
}
