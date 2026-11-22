package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Escrita do worker. Sempre INSERT ... ON CONFLICT DO NOTHING: o historico nunca e sobrescrito e
 * reexecutar um job nao duplica linhas (ADR 0013). Retorna 1 se gravou, 0 se ja existia.
 */
@Repository
public class GravacaoDados {

  private final JdbcTemplate jdbc;

  public GravacaoDados(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int inserirCotacao(
      String base, String destino, BigDecimal valor, OffsetDateTime dataHora, String fonte) {
    return jdbc.update(
        """
        INSERT INTO cotacao (moeda_base_id, moeda_destino_id, valor, data_hora, fonte_id)
        SELECT b.id, d.id, ?, ?, f.id
        FROM moeda b, moeda d, fonte_dados f
        WHERE b.codigo_iso = ? AND d.codigo_iso = ? AND f.codigo = ?
        ON CONFLICT DO NOTHING
        """,
        valor,
        Timestamp.from(dataHora.toInstant()),
        base,
        destino,
        fonte);
  }

  public int inserirIndicador(
      String pais, TipoIndicador tipo, BigDecimal valor, LocalDate periodo, String fonte) {
    return jdbc.update(
        """
        INSERT INTO indicador_economico (pais_id, tipo, valor, periodo, fonte_id)
        SELECT p.id, ?, ?, ?, f.id
        FROM pais p, fonte_dados f
        WHERE p.codigo_iso = ? AND f.codigo = ?
        ON CONFLICT DO NOTHING
        """,
        tipo.name(),
        valor,
        periodo,
        pais,
        fonte);
  }

  public int inserirIndice(
      String pais,
      BigDecimal indiceRisco,
      BigDecimal fatorAjuste,
      LocalDate dataReferencia,
      String fonte) {
    return jdbc.update(
        """
        INSERT INTO indice_geopolitico (pais_id, indice_risco, fator_ajuste, data_referencia, fonte_id)
        SELECT p.id, ?, ?, ?, f.id
        FROM pais p, fonte_dados f
        WHERE p.codigo_iso = ? AND f.codigo = ?
        ON CONFLICT DO NOTHING
        """,
        indiceRisco,
        fatorAjuste,
        dataReferencia,
        pais,
        fonte);
  }

  /** Cria a particao mensal de cotacao que contem o mes informado (funcao da V2). */
  public String criarParticaoCotacao(LocalDate mes) {
    return jdbc.queryForObject("SELECT criar_particao_cotacao(?)", String.class, mes);
  }

  public boolean fonteAtiva(String codigo) {
    Boolean ativa =
        jdbc.query(
            "SELECT ativa FROM fonte_dados WHERE codigo = ?",
            rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE,
            codigo);
    return Boolean.TRUE.equals(ativa);
  }
}
