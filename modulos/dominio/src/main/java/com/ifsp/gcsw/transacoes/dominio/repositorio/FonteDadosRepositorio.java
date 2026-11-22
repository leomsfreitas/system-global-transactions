package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.FonteDados;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FonteDadosRepositorio extends JpaRepository<FonteDados, Long> {
  Optional<FonteDados> findByCodigo(String codigo);
}
