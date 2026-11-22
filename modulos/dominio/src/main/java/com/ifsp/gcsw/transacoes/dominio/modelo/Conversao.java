package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Linha de auditoria gravada a cada conversao bem-sucedida (secao 5.11, ADR 0014). */
@Entity
@Table(name = "conversao")
public class Conversao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "moeda_origem_id", nullable = false)
  private Long moedaOrigemId;

  @Column(name = "moeda_destino_id", nullable = false)
  private Long moedaDestinoId;

  @Column(name = "valor_original", nullable = false)
  private BigDecimal valorOriginal;

  @Column(name = "taxa_aplicada", nullable = false)
  private BigDecimal taxaAplicada;

  @Column(name = "fator_geopolitico", nullable = false)
  private BigDecimal fatorGeopolitico;

  @Column(name = "valor_convertido", nullable = false)
  private BigDecimal valorConvertido;

  @Column(name = "data_hora", nullable = false)
  private OffsetDateTime dataHora;

  @Column(name = "fonte_id")
  private Long fonteId;

  @Column(name = "api_key_hash_prefixo", length = 8)
  private String apiKeyHashPrefixo;

  protected Conversao() {}

  public Conversao(
      Long moedaOrigemId,
      Long moedaDestinoId,
      BigDecimal valorOriginal,
      BigDecimal taxaAplicada,
      BigDecimal fatorGeopolitico,
      BigDecimal valorConvertido,
      OffsetDateTime dataHora,
      Long fonteId,
      String apiKeyHashPrefixo) {
    this.moedaOrigemId = moedaOrigemId;
    this.moedaDestinoId = moedaDestinoId;
    this.valorOriginal = valorOriginal;
    this.taxaAplicada = taxaAplicada;
    this.fatorGeopolitico = fatorGeopolitico;
    this.valorConvertido = valorConvertido;
    this.dataHora = dataHora;
    this.fonteId = fonteId;
    this.apiKeyHashPrefixo = apiKeyHashPrefixo;
  }

  public Long getId() {
    return id;
  }

  public Long getMoedaOrigemId() {
    return moedaOrigemId;
  }

  public Long getMoedaDestinoId() {
    return moedaDestinoId;
  }

  public BigDecimal getValorOriginal() {
    return valorOriginal;
  }

  public BigDecimal getTaxaAplicada() {
    return taxaAplicada;
  }

  public BigDecimal getFatorGeopolitico() {
    return fatorGeopolitico;
  }

  public BigDecimal getValorConvertido() {
    return valorConvertido;
  }

  public OffsetDateTime getDataHora() {
    return dataHora;
  }

  public Long getFonteId() {
    return fonteId;
  }

  public String getApiKeyHashPrefixo() {
    return apiKeyHashPrefixo;
  }
}
