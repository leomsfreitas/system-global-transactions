package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.Cotacao;
import com.ifsp.gcsw.transacoes.dominio.modelo.CotacaoId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Consultas nativas: aproveitam o indice unico (base, destino, data_hora) de cada particao. */
public interface CotacaoRepositorio extends JpaRepository<Cotacao, CotacaoId> {

  String SELECAO =
      """
      SELECT c.valor AS valor, c.data_hora AS dataHora, f.codigo AS fonte, f.id AS fonteId
      FROM cotacao c
      JOIN moeda b ON b.id = c.moeda_base_id
      JOIN moeda d ON d.id = c.moeda_destino_id
      JOIN fonte_dados f ON f.id = c.fonte_id
      WHERE b.codigo_iso = :base AND d.codigo_iso = :destino
        AND (CAST(:fonte AS VARCHAR) IS NULL OR f.codigo = CAST(:fonte AS VARCHAR))
      """;

  @Query(value = SELECAO + " ORDER BY c.data_hora DESC LIMIT 1", nativeQuery = true)
  Optional<CotacaoProjecao> maisRecente(String base, String destino, String fonte);

  @Query(
      value =
          SELECAO
              + " AND c.data_hora >= :inicio AND c.data_hora < :fim ORDER BY c.data_hora DESC LIMIT 1",
      nativeQuery = true)
  Optional<CotacaoProjecao> ultimaNoIntervalo(
      String base, String destino, String fonte, Instant inicio, Instant fim);

  @Query(
      value =
          """
          SELECT DISTINCT ON (CAST(timezone('UTC', c.data_hora) AS DATE))
                 c.valor AS valor, c.data_hora AS dataHora, f.codigo AS fonte, f.id AS fonteId
          FROM cotacao c
          JOIN moeda b ON b.id = c.moeda_base_id
          JOIN moeda d ON d.id = c.moeda_destino_id
          JOIN fonte_dados f ON f.id = c.fonte_id
          WHERE b.codigo_iso = :base AND d.codigo_iso = :destino
            AND (CAST(:fonte AS VARCHAR) IS NULL OR f.codigo = CAST(:fonte AS VARCHAR))
            AND c.data_hora >= :inicio AND c.data_hora < :fim
          ORDER BY CAST(timezone('UTC', c.data_hora) AS DATE), c.data_hora DESC
          """,
      nativeQuery = true)
  List<CotacaoProjecao> ultimasPorDia(
      String base, String destino, String fonte, Instant inicio, Instant fim);
}
