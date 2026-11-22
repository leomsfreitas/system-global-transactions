package com.ifsp.gcsw.transacoes.conectores.modelo;

/** Pais a consultar: ISO 3166 alfa-2 (banco, Banco Mundial) e alfa-3 (FMI). */
public record PaisConsulta(String iso2, String iso3) {}
