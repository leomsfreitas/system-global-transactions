package com.ifsp.gcsw.transacoes.conectores;

import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.util.List;

/** Estabilidade politica (WGI PV.EST, escala aproximada -2,5 a +2,5). */
public interface FonteRiscoPolitico {

  String codigo();

  ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises);
}
