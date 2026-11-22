package com.ifsp.gcsw.transacoes.dominio.servico;

/** Moeda suportada, como fica no cache moedas:lista. */
public record MoedaCadastrada(
    Long id, String codigo, String nome, int casasDecimais, String pais) {}
