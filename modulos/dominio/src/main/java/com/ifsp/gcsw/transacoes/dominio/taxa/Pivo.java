package com.ifsp.gcsw.transacoes.dominio.taxa;

/** Moeda pivo da taxa cruzada e a fonte cujas cotacoes a compoem (secao 5.4). */
public record Pivo(String moeda, String fonte) {}
