package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.IndiceGeopolitico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndiceGeopoliticoRepositorio extends JpaRepository<IndiceGeopolitico, Long> {

  @Query(
      value =
          """
          SELECT g.indice_risco AS indiceRisco, g.fator_ajuste AS fatorAjuste, g.data_referencia AS dataReferencia
          FROM indice_geopolitico g
          JOIN pais p ON p.id = g.pais_id
          WHERE p.codigo_iso = :pais
          ORDER BY g.data_referencia DESC, g.id DESC
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<IndiceProjecao> maisRecente(String pais);
}
