package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.IndicadorEconomico;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndicadorEconomicoRepositorio extends JpaRepository<IndicadorEconomico, Long> {

  /** O valor mais recente de cada tipo pedido para o pais (empate de periodo: ultimo gravado). */
  @Query(
      value =
          """
          SELECT DISTINCT ON (i.tipo) i.tipo AS tipo, i.valor AS valor, i.periodo AS periodo, f.codigo AS fonte
          FROM indicador_economico i
          JOIN pais p ON p.id = i.pais_id
          JOIN fonte_dados f ON f.id = i.fonte_id
          WHERE p.codigo_iso = :pais AND i.tipo IN (:tipos)
          ORDER BY i.tipo, i.periodo DESC, i.id DESC
          """,
      nativeQuery = true)
  List<IndicadorProjecao> maisRecentesPorTipo(String pais, Collection<String> tipos);
}
