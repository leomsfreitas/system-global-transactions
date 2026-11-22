package com.ifsp.gcsw.transacoes.dominio.motor;

import java.math.MathContext;
import java.math.RoundingMode;

/** Regras de arredondamento da secao 5.5 (ADR 0010). */
public final class Arredondamento {

  public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
  public static final MathContext DIVISAO = new MathContext(16, MODO);
  public static final int ESCALA_TAXA = 6;
  public static final int ESCALA_FATOR = 4;
  public static final int ESCALA_INDICE = 2;

  private Arredondamento() {}
}
