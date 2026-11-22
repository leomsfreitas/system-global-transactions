package com.ifsp.gcsw.transacoes.conectores;

import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.util.List;

/** Inflacao, juros e PIB. */
public interface FonteIndicadores {

  String codigo();

  ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises);
}
