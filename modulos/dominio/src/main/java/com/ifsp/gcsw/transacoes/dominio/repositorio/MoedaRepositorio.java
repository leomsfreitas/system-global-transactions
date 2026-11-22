package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.Moeda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MoedaRepositorio extends JpaRepository<Moeda, Long> {

  @Query("select m from Moeda m join fetch m.pais where m.codigoIso = :codigo")
  Optional<Moeda> buscarPorCodigo(String codigo);

  @Query("select m from Moeda m join fetch m.pais order by m.codigoIso")
  List<Moeda> listarComPais();
}
