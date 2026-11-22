package com.ifsp.gcsw.transacoes.conectores;

import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.time.LocalDate;
import java.util.Set;

public interface FonteCotacoes {

  /** Codigo da fonte em fonte_dados.codigo. */
  String codigo();

  ResultadoColeta<CotacaoColetada> buscar(LocalDate inicio, LocalDate fim, Set<String> moedas);
}
